
package com.shivacode.backend.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> buckets =
            new ConcurrentHashMap<>();

    private Bucket createBucket(
            int capacity,
            Duration refillDuration
    ) {

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(capacity, refillDuration)
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private boolean allowRequest(
            String key,
            int capacity,
            Duration refillDuration
    ) {

        Bucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> createBucket(capacity, refillDuration)
        );

        return bucket.tryConsume(1);
    }

    // Registration: 3 requests per minute
    public boolean allowRegistration(String ipAddress) {
        return allowRequest(
                "register:" + ipAddress,
                3,
                Duration.ofMinutes(1)
        );
    }

    // Login: 5 requests per minute
    public boolean allowLogin(String ipAddress) {
        return allowRequest(
                "login:" + ipAddress,
                5,
                Duration.ofMinutes(1)
        );
    }

    // Run Code: 10 requests per minute
    public boolean allowCodeExecution(String ipAddress) {
        return allowRequest(
                "execute:" + ipAddress,
                10,
                Duration.ofMinutes(1)
        );
    }

    // Batch execution: 5 requests per minute
    public boolean allowBatchExecution(String ipAddress) {
        return allowRequest(
                "batch:" + ipAddress,
                5,
                Duration.ofMinutes(1)
        );
    }

    // Submit solution: 5 requests per minute
    public boolean allowSubmission(String ipAddress) {
        return allowRequest(
                "submit:" + ipAddress,
                5,
                Duration.ofMinutes(1)
        );
    }
}

