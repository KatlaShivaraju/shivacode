
package com.shivacode.backend.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitFilterTest {

    private RateLimitService rateLimitService;
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService();
        rateLimitFilter = new RateLimitFilter(rateLimitService);
    }

    private MockHttpServletResponse sendRequest(String method, String path)
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setServletPath(path);
        request.setRemoteAddr("127.0.0.1");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = (req, res) -> {
            // Request is allowed to continue.
        };

        rateLimitFilter.doFilter(request, response, chain);

        return response;
    }

    @Test
    void registrationShouldBlockFourthRequest() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertEquals(200,
                    sendRequest("POST", "/api/auth/register").getStatus());
        }

        assertEquals(429,
                sendRequest("POST", "/api/auth/register").getStatus());
    }

    @Test
    void loginShouldBlockSixthRequest() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200,
                    sendRequest("POST", "/api/auth/login").getStatus());
        }

        assertEquals(429,
                sendRequest("POST", "/api/auth/login").getStatus());
    }

    @Test
    void executionShouldBlockEleventhRequest() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertEquals(200,
                    sendRequest("POST", "/api/execute").getStatus());
        }

        assertEquals(429,
                sendRequest("POST", "/api/execute").getStatus());
    }

    @Test
    void batchExecutionShouldBlockSixthRequest() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200,
                    sendRequest("POST", "/api/execute/batch").getStatus());
        }

        assertEquals(429,
                sendRequest("POST", "/api/execute/batch").getStatus());
    }

    @Test
    void submissionShouldBlockSixthRequest() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals(200,
                    sendRequest("POST", "/api/problems/1/submit").getStatus());
        }

        assertEquals(429,
                sendRequest("POST", "/api/problems/1/submit").getStatus());
    }

    @Test
    void getRequestsShouldNotBeRateLimited() throws Exception {
        for (int i = 0; i < 15; i++) {
            assertEquals(200,
                    sendRequest("GET", "/api/problems").getStatus());
        }
    }
}

