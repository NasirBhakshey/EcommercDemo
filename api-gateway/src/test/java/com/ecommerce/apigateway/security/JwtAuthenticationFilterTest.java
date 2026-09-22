package com.ecommerce.apigateway.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WebFilterChain chain;

    @Mock
    private Claims claims;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtUtil);
    }

    @Test
    void publicPath_shouldSkipJwtValidation() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .post("/auth/login")
                                .build()
                );

        when(chain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void missingAuthorizationHeader_shouldContinueWithoutAuthentication() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/users/profile")
                                .build()
                );

        when(chain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void nonBearerAuthorizationHeader_shouldContinueWithoutAuthentication() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/users/profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Basic abc123"
                                )
                                .build()
                );

        when(chain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        verify(chain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void invalidToken_shouldContinueWithoutAuthentication() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/users/profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer invalid-token"
                                )
                                .build()
                );

        when(jwtUtil.isTokenValid("invalid-token"))
                .thenReturn(false);

        when(chain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        verify(jwtUtil)
                .isTokenValid("invalid-token");

        verify(jwtUtil, never())
                .validateAndGetClaims(anyString());

        verify(chain).filter(exchange);
    }

    @Test
    void validToken_shouldCreateAuthenticationWithRoles() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/users/profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer valid-token"
                                )
                                .build()
                );

        when(jwtUtil.isTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtUtil.validateAndGetClaims("valid-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("admin@test.com");

        when(claims.get("roles", List.class))
                .thenReturn(List.of("ADMIN", "USER"));

        AtomicReference<Authentication> capturedAuthentication =
                new AtomicReference<>();

        when(chain.filter(exchange))
                .thenReturn(
                        ReactiveSecurityContextHolder
                                .getContext()
                                .doOnNext(securityContext ->
                                        capturedAuthentication.set(
                                                securityContext.getAuthentication()
                                        )
                                )
                                .then()
                );

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        Authentication authentication =
                capturedAuthentication.get();

        assertNotNull(authentication);

        assertEquals(
                "admin@test.com",
                authentication.getPrincipal()
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_ADMIN")
                        )
        );

        assertTrue(
                authentication.getAuthorities()
                        .stream()
                        .anyMatch(authority ->
                                authority.getAuthority()
                                        .equals("ROLE_USER")
                        )
        );

        verify(jwtUtil).isTokenValid("valid-token");
        verify(jwtUtil).validateAndGetClaims("valid-token");
        verify(chain).filter(exchange);
    }

    @Test
    void validTokenWithNoRoles_shouldCreateAuthenticationWithoutAuthorities() {

        MockServerWebExchange exchange =
                MockServerWebExchange.from(
                        MockServerHttpRequest
                                .get("/users/profile")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer valid-token"
                                )
                                .build()
                );

        when(jwtUtil.isTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtUtil.validateAndGetClaims("valid-token"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("user@test.com");

        when(claims.get("roles", List.class))
                .thenReturn(null);

        AtomicReference<Authentication> capturedAuthentication =
                new AtomicReference<>();

        when(chain.filter(exchange))
                .thenReturn(
                        Mono.deferContextual(contextView ->
                                ReactiveSecurityContextHolder
                                        .getContext()
                                        .doOnNext(context ->
                                                capturedAuthentication.set(
                                                        context.getAuthentication()
                                                )
                                        )
                                        .then()
                        )
                );

        StepVerifier.create(
                filter.filter(exchange, chain)
        ).verifyComplete();

        assertNotNull(capturedAuthentication.get());

        assertTrue(
                capturedAuthentication
                        .get()
                        .getAuthorities()
                        .isEmpty()
        );
    }
}
