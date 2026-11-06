
package com.shivacode.backend.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitServiceTest {

    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
    }

    @Test
    void registrationShouldAllowOnlyThreeRequests() {

        String ip = "192.168.1.10";

        assertTrue(rateLimitService.allowRegistration(ip));
        assertTrue(rateLimitService.allowRegistration(ip));
        assertTrue(rateLimitService.allowRegistration(ip));

        assertFalse(rateLimitService.allowRegistration(ip));
    }

    @Test
    void loginShouldAllowOnlyFiveRequests() {

        String ip = "192.168.1.20";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowLogin(ip));
        }

        assertFalse(rateLimitService.allowLogin(ip));
    }

    @Test
    void codeExecutionShouldAllowTenRequests() {

        String ip = "192.168.1.30";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.allowCodeExecution(ip));
        }

        assertFalse(rateLimitService.allowCodeExecution(ip));
    }

    @Test
    void batchExecutionShouldAllowFiveRequests() {

        String ip = "192.168.1.40";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowBatchExecution(ip));
        }

        assertFalse(rateLimitService.allowBatchExecution(ip));
    }

    @Test
    void submissionShouldAllowFiveRequests() {

        String ip = "192.168.1.50";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowSubmission(ip));
        }

        assertFalse(rateLimitService.allowSubmission(ip));
    }

    @Test
    void differentIpsShouldHaveSeparateLimits() {

        assertTrue(rateLimitService.allowLogin("192.168.1.1"));
        assertTrue(rateLimitService.allowLogin("192.168.1.2"));
    }

    @Test
    void differentEndpointsShouldHaveSeparateLimits() {

        String ip = "192.168.1.60";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowLogin(ip));
        }

        assertFalse(rateLimitService.allowLogin(ip));

        assertTrue(rateLimitService.allowCodeExecution(ip));
    }
}