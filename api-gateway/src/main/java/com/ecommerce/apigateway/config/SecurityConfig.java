package com.ecommerce.apigateway.config;

import com.ecommerce.apigateway.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            ServerAuthenticationEntryPoint authenticationEntryPoint,
            ServerAccessDeniedHandler accessDeniedHandler) {

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .authorizeExchange(exchange -> exchange

                        // Public authentication APIs
                        .pathMatchers(
                                "/auth/login",
                                "/auth/register",
                                "/actuator/health",
                                "/actuator/gateway/**"
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

                        .pathMatchers(HttpMethod.GET,"/product/**")
                        .hasAnyRole("ADMIN", "MANAGER", "USER")

                        .pathMatchers(HttpMethod.POST, "/product/**")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.PUT, "/product/**")
                        .hasAnyRole("MANAGER", "ADMIN")

                        .pathMatchers(HttpMethod.DELETE, "/product/**")
                        .hasRole("ADMIN")

                        .pathMatchers(HttpMethod.POST, "/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "USER")

                        .pathMatchers(HttpMethod.GET, "/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "USER")

                        .pathMatchers(HttpMethod.PATCH, "/orders/**")
                        .hasAnyRole("ADMIN", "MANAGER", "USER")
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

    @Bean
    public ServerAuthenticationEntryPoint authenticationEntryPoint() {

        return (exchange, exception) -> {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            exchange.getResponse()
                    .getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            String body = """
                {
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "Authentication is required"
                }
                """;

            byte[] bytes =
                    body.getBytes(StandardCharsets.UTF_8);

            return exchange.getResponse()
                    .writeWith(
                            Mono.just(
                                    exchange.getResponse()
                                            .bufferFactory()
                                            .wrap(bytes)
                            )
                    );
        };
    }

    @Bean
    public ServerAccessDeniedHandler accessDeniedHandler() {

        return (exchange, exception) -> {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.FORBIDDEN);

            exchange.getResponse()
                    .getHeaders()
                    .setContentType(MediaType.APPLICATION_JSON);

            String body = """
                {
                  "status": 403,
                  "error": "Forbidden",
                  "message": "You do not have permission to access this resource"
                }
                """;

            byte[] bytes =
                    body.getBytes(StandardCharsets.UTF_8);

            return exchange.getResponse()
                    .writeWith(
                            Mono.just(
                                    exchange.getResponse()
                                            .bufferFactory()
                                            .wrap(bytes)
                            )
                    );
        };
    }
}
