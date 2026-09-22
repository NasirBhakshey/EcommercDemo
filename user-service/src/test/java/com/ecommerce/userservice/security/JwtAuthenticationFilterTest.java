package com.ecommerce.userservice.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_shouldContinueWhenAuthorizationHeaderIsMissing()
            throws Exception {

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        verify(filterChain)
                .doFilter(request, response);

        verifyNoInteractions(jwtService);

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );
    }

    @Test
    void doFilter_shouldAuthenticateWhenTokenIsValid()
            throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer valid-token"
        );

        when(jwtService.isTokenValid("valid-token"))
                .thenReturn(true);

        when(jwtService.extractEmail("valid-token"))
                .thenReturn("user@test.com");

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertNotNull(authentication);

        assertEquals(
                "user@test.com",
                authentication.getPrincipal()
        );

        assertTrue(authentication.isAuthenticated());

        verify(jwtService)
                .isTokenValid("valid-token");

        verify(jwtService)
                .extractEmail("valid-token");

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void doFilter_shouldNotAuthenticateWhenTokenIsInvalid()
            throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer invalid-token"
        );

        when(jwtService.isTokenValid("invalid-token"))
                .thenReturn(false);

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(jwtService)
                .isTokenValid("invalid-token");

        verify(jwtService, never())
                .extractEmail(anyString());

        verify(filterChain)
                .doFilter(request, response);
    }

    @Test
    void doFilter_shouldClearSecurityContextWhenJwtProcessingThrowsException()
            throws Exception {

        request.addHeader(
                "Authorization",
                "Bearer broken-token"
        );

        when(jwtService.isTokenValid("broken-token"))
                .thenThrow(
                        new RuntimeException("JWT processing failed")
                );

        jwtAuthenticationFilter.doFilter(
                request,
                response,
                filterChain
        );

        assertNull(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        );

        verify(filterChain)
                .doFilter(request, response);
    }
}
