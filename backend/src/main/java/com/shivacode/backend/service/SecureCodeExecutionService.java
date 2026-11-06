
package com.shivacode.backend.service;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SecureCodeExecutionService {

    private static final int MAX_OUTPUT_BYTES = 1024 * 1024;

    // Request validation limits
    private static final int MAX_CODE_BYTES = 100 * 1024;
    private static final int MAX_INPUT_BYTES = 64 * 1024;
    private static final int MAX_TOTAL_INPUT_BYTES = 256 * 1024;

    // Maximum 2 concurrent judge requests per backend instance.
    private final Semaphore executionSlots = new Semaphore(2, true);

    @Value("${shivacode.runner.image:shivacode-java-runner:latest}")
    private String runnerImage;

    @Value("${shivacode.runner.timeout-seconds:5}")
    private int timeoutSeconds;

    public CodeExecutionResponse execute(CodeExecutionRequest request) {

        String input = request == null || request.input() == null
                ? ""
                : request.input();

        return executeBatch(request, List.of(input)).get(0);
    }

    public List<CodeExecutionResponse> executeBatch(
            CodeExecutionRequest request,
            List<String> inputs
    ) {

        int count = inputs == null || inputs.isEmpty()
                ? 1
                : inputs.size();

        if (request == null || request.code() == null
                || request.code().isBlank()) {

            return repeated(
                    count,
                    "COMPILATION_ERROR",
                    "",
                    "Code cannot be empty.",
                    0
            );
        }

        if (request.language() == null
                || !request.language().equalsIgnoreCase("java")) {

            return repeated(
                    count,
                    "ERROR",
                    "",
                    "Only Java is supported.",
                    0
            );
        }

        // Validate request sizes before creating files or Docker containers.
        String validationError = validateRequest(request, inputs);

        if (validationError != null) {
            return repeated(
                    count,
                    "ERROR",
                    "",
                    validationError,
                    0
            );
        }

        if (inputs == null || inputs.isEmpty()) {
            return List.of(response("EXECUTED", "", "", 0));
        }

        Path workspace = null;
        boolean slotAcquired = false;

        try {

            // Allow up to 2 concurrent judge requests.
            slotAcquired = executionSlots.tryAcquire(
                    1,
                    TimeUnit.SECONDS
            );

            if (!slotAcquired) {
                return repeated(
                        count,
                        "ERROR",
                        "",
                        "Judge is busy. Please try again shortly.",
                        0
                );
            }

            workspace = Files.createTempDirectory(
                    "shivacode-secure-"
            );

            Path sourceDir = Files.createDirectory(
                    workspace.resolve("source")
            );

            Path classesDir = Files.createDirectory(
                    workspace.resolve("classes")
            );

            Files.writeString(
                    sourceDir.resolve("Solution.java"),
                    request.code(),
                    StandardCharsets.UTF_8
            );

            // Stage 1: Compile submitted code.
            long compileStart = System.nanoTime();

            ProcessResult compilation = runDocker(
                    List.of(
                            "docker", "run", "-i",
                            "--network", "none",
                            "--memory", "256m",
                            "--memory-swap", "256m",
                            "--cpus", "0.5",
                            "--pids-limit", "64",
                            "--read-only",
                            "--cap-drop", "ALL",
                            "--security-opt", "no-new-privileges",
                            "--user", "10001:10001",
                            "--mount",
                            "type=bind,source=" + sourceDir.toAbsolutePath()
                                    + ",target=/source,readonly",
                            "--mount",
                            "type=bind,source=" + classesDir.toAbsolutePath()
                                    + ",target=/classes",
                            "--tmpfs", "/tmp:rw,nosuid,size=32m",
                            runnerImage,
                            "javac",
                            "-proc:none",
                            "-d", "/classes",
                            "/source/Solution.java"
                    ),
                    "",
                    timeoutSeconds + 5L
            );

            long compileTime = elapsed(compileStart);

            if (compilation.oomKilled()) {
                return repeated(
                        count,
                        "MEMORY_LIMIT_EXCEEDED",
                        "",
                        "Compilation exceeded the memory limit.",
                        compileTime
                );
            }

            if (compilation.timedOut()) {
                return repeated(
                        count,
                        "TIME_LIMIT_EXCEEDED",
                        "",
                        "Compilation timed out.",
                        compileTime
                );
            }

            if (compilation.outputLimitExceeded()) {
                return repeated(
                        count,
                        "COMPILATION_ERROR",
                        "",
                        "Compiler output exceeded the output limit.",
                        compileTime
                );
            }

            if (compilation.exitCode() != 0) {
                return repeated(
                        count,
                        "COMPILATION_ERROR",
                        "",
                        combine(
                                compilation.stdout(),
                                compilation.stderr()
                        ),
                        compileTime
                );
            }

            // Stage 2: Run each test in a fresh container.
            List<CodeExecutionResponse> results = new ArrayList<>();

            for (String input : inputs) {

                long caseStart = System.nanoTime();

                ProcessResult execution = runDocker(
                        List.of(
                                "docker", "run", "-i",
                                "--network", "none",
                                "--memory", "128m",
                                "--memory-swap", "128m",
                                "--cpus", "0.5",
                                "--pids-limit", "64",
                                "--read-only",
                                "--cap-drop", "ALL",
                                "--security-opt", "no-new-privileges",
                                "--user", "10001:10001",
                                "--mount",
                                "type=bind,source=" + classesDir.toAbsolutePath()
                                        + ",target=/classes,readonly",
                                "--tmpfs",
                                "/tmp:rw,noexec,nosuid,size=16m",
                                runnerImage,
                                "java",
                                "-Djava.io.tmpdir=/tmp",
                                "-cp", "/classes",
                                "Solution"
                        ),
                        input == null ? "" : input,
                        timeoutSeconds
                );

                long caseTime = elapsed(caseStart);

                if (execution.oomKilled()) {

                    results.add(response(
                            "MEMORY_LIMIT_EXCEEDED",
                            "",
                            "Program exceeded the 128 MB memory limit.",
                            caseTime
                    ));

                } else if (execution.timedOut()) {

                    results.add(response(
                            "TIME_LIMIT_EXCEEDED",
                            execution.stdout(),
                            "Program exceeded the time limit.",
                            caseTime
                    ));

                } else if (execution.outputLimitExceeded()) {

                    results.add(response(
                            "RUNTIME_ERROR",
                            "",
                            "Program output exceeded the 1 MB limit.",
                            caseTime
                    ));

                } else if (execution.exitCode() != 0) {

                    String errorOutput = execution.stderr();

                    if (isMemoryError(errorOutput)) {

                        results.add(response(
                                "MEMORY_LIMIT_EXCEEDED",
                                execution.stdout(),
                                "Program exceeded the Java memory limit.",
                                caseTime
                        ));

                    } else {

                        results.add(response(
                                "RUNTIME_ERROR",
                                execution.stdout(),
                                errorOutput.isBlank()
                                        ? "Program exited with code "
                                        + execution.exitCode()
                                        : errorOutput,
                                caseTime
                        ));
                    }

                } else {

                    results.add(response(
                            "EXECUTED",
                            execution.stdout(),
                            execution.stderr(),
                            caseTime
                    ));
                }
            }

            return results;

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return repeated(
                    count,
                    "ERROR",
                    "",
                    "Execution was interrupted.",
                    0
            );

        } catch (Exception e) {

            return repeated(
                    count,
                    "ERROR",
                    "",
                    "Secure runner error: " + e.getMessage(),
                    0
            );

        } finally {

            // Delete temporary source and compiled files.
            if (workspace != null) {
                deleteDirectory(workspace);
            }

            // Always release an acquired execution slot.
            if (slotAcquired) {
                executionSlots.release();
            }
        }
    }

    // Validate source code and test input sizes using UTF-8 byte counts.
    private String validateRequest(
            CodeExecutionRequest request,
            List<String> inputs
    ) {

        if (exceedsSize(request.code(), MAX_CODE_BYTES)) {
            return "Source code exceeds the 100 KB limit.";
        }

        if (inputs == null) {
            return null;
        }

        long totalInputBytes = 0;

        for (String input : inputs) {

            if (input == null) {
                continue;
            }

            int inputBytes = input.getBytes(
                    StandardCharsets.UTF_8
            ).length;

            if (inputBytes > MAX_INPUT_BYTES) {
                return "A test input exceeds the 64 KB limit.";
            }

            totalInputBytes += inputBytes;

            if (totalInputBytes > MAX_TOTAL_INPUT_BYTES) {
                return "Combined test inputs exceed the 256 KB limit.";
            }
        }

        return null;
    }

    private boolean exceedsSize(String value, int maxBytes) {

        return value != null
                && value.getBytes(StandardCharsets.UTF_8).length > maxBytes;
    }

    // Detect JVM heap exhaustion.
    private boolean isMemoryError(String errorOutput) {

        if (errorOutput == null || errorOutput.isBlank()) {
            return false;
        }

        return errorOutput.contains("OutOfMemoryError")
                || errorOutput.contains("Java heap space")
                || errorOutput.contains("GC overhead limit exceeded");
    }

    private ProcessResult runDocker(
            List<String> command,
            String input,
            long timeout
    ) throws IOException, InterruptedException,
            ExecutionException, TimeoutException {

        String containerName = "shivacode-"
                + UUID.randomUUID().toString().replace("-", "");

        List<String> dockerCommand = new ArrayList<>(command);

        int runIndex = dockerCommand.indexOf("run");

        dockerCommand.add(runIndex + 1, "--name");
        dockerCommand.add(runIndex + 2, containerName);

        Process process = new ProcessBuilder(dockerCommand).start();

        ExecutorService readers = Executors.newFixedThreadPool(2);

        AtomicInteger totalOutput = new AtomicInteger(0);
        AtomicBoolean outputExceeded = new AtomicBoolean(false);

        Future<String> stdoutFuture = readers.submit(
                () -> readLimited(
                        process.getInputStream(),
                        totalOutput,
                        outputExceeded
                )
        );

        Future<String> stderrFuture = readers.submit(
                () -> readLimited(
                        process.getErrorStream(),
                        totalOutput,
                        outputExceeded
                )
        );

        try {

            try {

                process.getOutputStream().write(
                        input.getBytes(StandardCharsets.UTF_8)
                );

                process.getOutputStream().close();

            } catch (IOException e) {

                if (process.isAlive()) {
                    throw e;
                }
            }

            long deadline = System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(timeout);

            while (process.isAlive()) {

                if (outputExceeded.get()) {

                    process.destroyForcibly();
                    process.waitFor();

                    return new ProcessResult(
                            -1, "", "", false, true, false
                    );
                }

                long remaining = deadline - System.nanoTime();

                if (remaining <= 0) {

                    process.destroyForcibly();
                    process.waitFor();

                    return new ProcessResult(
                            -1, "", "", true, false, false
                    );
                }

                process.waitFor(
                        Math.min(
                                remaining,
                                TimeUnit.MILLISECONDS.toNanos(50)
                        ),
                        TimeUnit.NANOSECONDS
                );
            }

            String stdout = getCapturedOutput(stdoutFuture);
            String stderr = getCapturedOutput(stderrFuture);

            // Inspect OOM status before removing the container.
            boolean oomKilled = wasContainerOomKilled(containerName);

            if (outputExceeded.get()) {
                return new ProcessResult(
                        -1, stdout, stderr, false, true, oomKilled
                );
            }

            return new ProcessResult(
                    process.exitValue(),
                    stdout,
                    stderr,
                    false,
                    false,
                    oomKilled
            );

        } finally {

            if (process.isAlive()) {
                process.destroyForcibly();
            }

            readers.shutdownNow();

            // Remove container after inspection.
            stopContainer(containerName);
        }
    }

    private boolean wasContainerOomKilled(String containerName) {

        Process inspect = null;

        try {

            inspect = new ProcessBuilder(
                    "docker",
                    "inspect",
                    "--format={{.State.OOMKilled}}",
                    containerName
            ).redirectErrorStream(true).start();

            String result = new String(
                    inspect.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            ).trim();

            if (!inspect.waitFor(3, TimeUnit.SECONDS)) {
                inspect.destroyForcibly();
                return false;
            }

            return "true".equalsIgnoreCase(result);

        } catch (Exception e) {

            if (inspect != null && inspect.isAlive()) {
                inspect.destroyForcibly();
            }

            return false;
        }
    }

    private String readLimited(
            InputStream stream,
            AtomicInteger totalOutput,
            AtomicBoolean outputExceeded
    ) throws IOException {

        try (
                stream;
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            byte[] buffer = new byte[8192];
            int read;

            while ((read = stream.read(buffer)) != -1) {

                int current = totalOutput.addAndGet(read);

                if (current > MAX_OUTPUT_BYTES) {
                    outputExceeded.set(true);
                    continue;
                }

                output.write(buffer, 0, read);
            }

            return output.toString(StandardCharsets.UTF_8);
        }
    }

    private String getCapturedOutput(Future<String> future)
            throws InterruptedException, ExecutionException,
            TimeoutException {

        return future.get(5, TimeUnit.SECONDS);
    }

    private void stopContainer(String name) {

        try {

            Process cleanup = new ProcessBuilder(
                    "docker", "rm", "-f", name
            ).redirectErrorStream(true).start();

            if (!cleanup.waitFor(5, TimeUnit.SECONDS)) {
                cleanup.destroyForcibly();
            }

        } catch (Exception ignored) {
            // Best-effort cleanup.
        }
    }

    private String combine(String stdout, String stderr) {

        if (stdout.isBlank()) {
            return stderr;
        }

        if (stderr.isBlank()) {
            return stdout;
        }

        return stdout + System.lineSeparator() + stderr;
    }

    private long elapsed(long start) {

        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - start
        );
    }

    private CodeExecutionResponse response(
            String status,
            String output,
            String error,
            long time
    ) {

        return new CodeExecutionResponse(
                status,
                output,
                error,
                time
        );
    }

    private List<CodeExecutionResponse> repeated(
            int count,
            String status,
            String output,
            String error,
            long time
    ) {

        List<CodeExecutionResponse> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            results.add(response(status, output, error, time));
        }

        return results;
    }

    private void deleteDirectory(Path directory) {

        try (var paths = Files.walk(directory)) {

            paths.sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {

                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            // Best-effort cleanup.
                        }
                    });

        } catch (IOException ignored) {
            // Best-effort cleanup.
        }
    }

    private record ProcessResult(
            int exitCode,
            String stdout,
            String stderr,
            boolean timedOut,
            boolean outputLimitExceeded,
            boolean oomKilled
    ) {}
}




