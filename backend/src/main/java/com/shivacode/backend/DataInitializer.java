
package com.shivacode.backend;

import com.shivacode.backend.service.TestCaseService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeTestCases(
            TestCaseService testCaseService
    ) {
        return args -> {
            System.out.println("=== ShivaCode test-case seeding started ===");

            testCaseService.seedTestCases();

            System.out.println("=== ShivaCode test-case seeding finished ===");
        };
    }
}


