
package com.shivacode.backend.config;

import com.shivacode.backend.ratelimit.RateLimitFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Prevent the filter from executing twice.
    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter rateLimitFilter
    ) {
        FilterRegistrationBean<RateLimitFilter> registration =
                new FilterRegistrationBean<>(rateLimitFilter);

        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RateLimitFilter rateLimitFilter
    ) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // Public authentication endpoints
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS, "/**"
                        ).permitAll()

                        // Public problem list
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems"
                        ).permitAll()

                        // Public problem details
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/{id}"
                        ).permitAll()

                        // Public test cases
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/{id}/test-cases"
                        ).permitAll()

                        // Authenticated submission history
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/problems/{id}/submissions"
                        ).authenticated()

                        // Everything else requires authentication
                        .anyRequest().authenticated()
                )

                // Register rate limiter inside Spring Security
                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT authentication
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(Customizer.withDefaults()))

                .build();
    }
}


