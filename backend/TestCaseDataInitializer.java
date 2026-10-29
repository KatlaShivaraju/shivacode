
package com.shivacode.backend;

import com.shivacode.backend.service.TestCaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestCaseDataInitializer {

    @Bean
    public CommandLineRunner initializeTestCases(
            TestCaseService testCaseService
    ) {
        return args -> testCaseService.seedTestCases();
    }
}

