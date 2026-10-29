
package com.shivacode.backend.service;

import com.shivacode.backend.dto.CodeExecutionRequest;
import com.shivacode.backend.dto.CodeExecutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class CodeExecutionService {

    @Value("${shivacode.runner.image:shivacode-java-runner:latest}")
    private String runnerImage;

    @Value("${shivacode.runner.timeout-seconds:5}")
    private int timeoutSeconds;

    public CodeExecutionResponse execute(CodeExecutionRequest request) {
        String input = request.input() == null ? "" : request.input();

        List<CodeExecutionResponse> results =
                executeBatch(request, List.of(input));

        return results.get(0);
    }

    public List<CodeExecutionResponse> executeBatch(
            CodeExecutionRequest request,
            List<String> inputs
    ) {
        if (request == null || request.code() == null
                || request.code().isBlank()) {
            return repeatedResponse(
                    safeCount(inputs),
                    "COMPILATION_ERROR",
                    "",
                    "Code cannot be empty.",
                    0
            );
        }

        if (request.language() == null
                || !request.language().equalsIgnoreCase("java")) {
            return repeatedResponse(
                    safeCount(inputs),
                    "ERROR",
                    "",
                    "Only Java is supported.",
                    0
            );
        }

        if (inputs == null || inputs.isEmpty()) {
            return List.of(response("EXECUTED", "", "", 0));
        }

        Path tempDirectory = null;
        String containerId = null;

        try {
            tempDirectory = Files.createTempDirectory("shivacode-");
            Path work = tempDirectory;

            Files.writeString(
                    work.resolve("Solution.java"),
                    request.code(),
                    StandardCharsets.UTF_8
            );

            for (int i = 0; i < inputs.size(); i++) {
                Files.writeString(
                        work.resolve("input-" + i + ".txt"),
                        inputs.get(i) == null ? "" : inputs.get(i),
                        StandardCharsets.UTF_8
                );
            }

            String script = buildExecutionScript(inputs.size());

            Process create = new ProcessBuilder(
                    "docker", "create",
                    "--network", "none",
                    "--memory", "128m",
                    "--cpus", "0.5",
                    "--pids-limit", "64",
                    "--read-only",
                    "--tmpfs", "/tmp:rw,noexec,nosuid,size=16m",
                    "--cap-drop", "ALL",
                    "--security-opt", "no-new-privileges",
                    "--user", "1000:1000",
                    "--mount",
                    "type=bind,source=" + work.toAbsolutePath()
                            + ",target=/runner/work",
                    "--workdir", "/runner/work",
                    runnerImage,
                    "sh", "-c", script
            ).redirectErrorStream(true).start();

            boolean createFinished = create.waitFor(20, TimeUnit.SECONDS);

            if (!createFinished) {
                create.destroyForcibly();

                return repeatedResponse(
                        inputs.size(),
                        "ERROR",
                        "",
                        "Timed out while creating the runner container.",
                        0
                );
            }

            String createOutput = new String(
                    create.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            if (create.exitValue() != 0) {
                return repeatedResponse(
                        inputs.size(),
                        "ERROR",
                        "",
                        "Docker container creation failed: " + createOutput,
                        0
                );
            }

            containerId = createOutput.trim();

            if (containerId.isBlank()) {
                return repeatedResponse(
                        inputs.size(),
                        "ERROR",
                        "",
                        "Docker did not return a container ID.",
                        0
                );
            }

            long start = System.nanoTime();

            Process run = new ProcessBuilder(
                    "docker", "start", "--attach", containerId
            ).redirectErrorStream(true).start();

            boolean finished = run.waitFor(
                    Math.max(15, (long) timeoutSeconds * inputs.size() + 10),
                    TimeUnit.SECONDS
            );

            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(
                    System.nanoTime() - start
            );

            if (!finished) {
                run.destroyForcibly();

                return repeatedResponse(
                        inputs.size(),
                        "TIME_LIMIT_EXCEEDED",
                        "",
                        "Execution exceeded the overall time limit.",
                        elapsedMs
                );
            }

            // Read the stream even if Docker returns a non-zero exit code.
            String runOutput = new String(
                    run.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            Path compileErrorFile = work.resolve("compile-error.txt");

            if (Files.exists(compileErrorFile)) {
                String compileError = Files.readString(
                        compileErrorFile,
                        StandardCharsets.UTF_8
                ).trim();

                if (!compileError.isEmpty()) {
                    return repeatedResponse(
                            inputs.size(),
                            "COMPILATION_ERROR",
                            "",
                            compileError,
                            elapsedMs
                    );
                }
            }

            List<CodeExecutionResponse> results = new ArrayList<>();

            for (int i = 0; i < inputs.size(); i++) {
                Path outputFile = work.resolve("output-" + i + ".txt");
                Path errorFile = work.resolve("error-" + i + ".txt");
                Path statusFile = work.resolve("status-" + i + ".txt");

                if (!Files.exists(statusFile)) {
                    results.add(response(
                            "ERROR",
                            "",
                            "Runner did not produce a status file. "
                                    + runOutput,
                            elapsedMs
                    ));
                    continue;
                }

                String status = Files.readString(
                        statusFile,
                        StandardCharsets.UTF_8
                ).trim();

                String output = Files.exists(outputFile)
                        ? Files.readString(
                        outputFile, StandardCharsets.UTF_8
                )
                        : "";

                String error = Files.exists(errorFile)
                        ? Files.readString(
                        errorFile, StandardCharsets.UTF_8
                ).trim()
                        : "";

                if ("124".equals(status)) {
                    results.add(response(
                            "TIME_LIMIT_EXCEEDED",
                            output,
                            "Program exceeded the time limit.",
                            elapsedMs
                    ));
                } else if (!"0".equals(status)) {
                    results.add(response(
                            "RUNTIME_ERROR",
                            output,
                            error.isBlank()
                                    ? "Program exited with code " + status
                                    : error,
                            elapsedMs
                    ));
                } else {
                    results.add(response(
                            "EXECUTED",
                            output,
                            error,
                            elapsedMs
                    ));
                }
            }

            return results;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return repeatedResponse(
                    inputs.size(),
                    "ERROR",
                    "",
                    "Execution was interrupted.",
                    0
            );

        } catch (IOException e) {
            return repeatedResponse(
                    inputs.size(),
                    "ERROR",
                    "",
                    "Runner service error: " + e.getMessage(),
                    0
            );

        } finally {
            if (containerId != null && !containerId.isBlank()) {
                try {
                    Process cleanup = new ProcessBuilder(
                            "docker", "rm", "-f", containerId
                    ).redirectErrorStream(true).start();

                    cleanup.waitFor(5, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // Best-effort container cleanup.
                }
            }

            if (tempDirectory != null) {
                try (var paths = Files.walk(tempDirectory)) {
                    paths.sorted((a, b) -> b.compareTo(a))
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                    // Best-effort temporary-file cleanup.
                                }
                            });
                } catch (IOException ignored) {
                    // Best-effort temporary-directory cleanup.
                }
            }
        }
    }

    private String buildExecutionScript(int inputCount) {
        StringBuilder script = new StringBuilder();

        script.append("timeout ")
                .append(timeoutSeconds)
                .append("s javac Solution.java 2> compile-error.txt; ")
                .append("compile_code=$?; ")
                .append("if [ $compile_code -eq 124 ]; then ")
                .append("echo 'Compilation timed out' > compile-error.txt; ")
                .append("exit 10; ")
                .append("fi; ")
                .append("if [ $compile_code -ne 0 ]; then exit 10; fi; ");

        for (int i = 0; i < inputCount; i++) {
            script.append("timeout ")
                    .append(timeoutSeconds)
                    .append("s java Solution ")
                    .append("< input-").append(i).append(".txt ")
                    .append("> output-").append(i).append(".txt ")
                    .append("2> error-").append(i).append(".txt; ")
                    .append("code=$?; ")
                    .append("echo $code > status-").append(i).append(".txt; ")
                    .append("if [ $code -eq 124 ]; then exit 0; fi; ");
        }

        return script.toString();
    }

    private CodeExecutionResponse response(
            String status,
            String output,
            String error,
            long executionTimeMs
    ) {
        return new CodeExecutionResponse(
                status,
                output,
                error,
                executionTimeMs
        );
    }

    private List<CodeExecutionResponse> repeatedResponse(
            int count,
            String status,
            String output,
            String error,
            long executionTimeMs
    ) {
        List<CodeExecutionResponse> results = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            results.add(response(
                    status, output, error, executionTimeMs
            ));
        }

        return results;
    }

    private int safeCount(List<String> inputs) {
        return inputs == null || inputs.isEmpty() ? 1 : inputs.size();
    }
}








