package com.ecommerce.apigateway.config;

import com.ecommerce.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .authorizeExchange(exchange -> exchange

                        // Public authentication APIs
                        .pathMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/actuator/health"
                        ).permitAll()

                        // User APIs require login
                        .pathMatchers("/users/**")
                        .hasAnyRole("USER", "MANAGER", "ADMIN")

                        .pathMatchers("/manager/**")
                        .hasAnyRole("MANAGER", "ADMIN")

                        .pathMatchers("/admin/**")
                        .hasAnyRole("ADMIN")

                        .pathMatchers("/roles/**")
                        .hasAnyRole("ADMIN")
                        // Everything else requires authentication
                        .anyExchange().authenticated()
                )

                // JWT filter
                .addFilterAt(
                        jwtAuthenticationFilter,
                        SecurityWebFiltersOrder.AUTHENTICATION
                )

                .build();
    }
}
