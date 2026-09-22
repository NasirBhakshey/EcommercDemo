package com.ecommerce.apigateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtUtilTest {

    private static final String SECRET =
            "ThisIsATestJwtSecretKeyOnlyForAutomatedTesting123456789";

    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {

        jwtUtil = new JwtUtil(SECRET);

        secretKey = Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String createToken(
            String email,
            Long userId,
            List<String> roles,
            long expirationMillis) {

        Date now = new Date();

        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(
                        new Date(now.getTime() + expirationMillis)
                )
                .signWith(secretKey)
                .compact();
    }

    @Test
    void isTokenValid_shouldReturnTrueForValidToken() {

        String token = createToken(
                "user@test.com",
                10L,
                List.of("USER"),
                600000
        );

        assertTrue(jwtUtil.isTokenValid(token));
    }

    @Test
    void extractEmail_shouldReturnSubject() {

        String token = createToken(
                "admin@test.com",
                1L,
                List.of("ADMIN"),
                600000
        );

        assertEquals(
                "admin@test.com",
                jwtUtil.extractEmail(token)
        );
    }

    @Test
    void extractUserId_shouldReturnUserId() {

        String token = createToken(
                "user@test.com",
                25L,
                List.of("USER"),
                600000
        );

        assertEquals(
                25L,
                jwtUtil.extractUserId(token)
        );
    }

    @Test
    void extractRoles_shouldReturnRoles() {

        String token = createToken(
                "manager@test.com",
                20L,
                List.of("MANAGER", "USER"),
                600000
        );

        List<String> roles =
                jwtUtil.extractRoles(token);

        assertNotNull(roles);
        assertEquals(2, roles.size());
        assertTrue(roles.contains("MANAGER"));
        assertTrue(roles.contains("USER"));
    }

    @Test
    void validateAndGetClaims_shouldReturnClaims() {

        String token = createToken(
                "admin@test.com",
                1L,
                List.of("ADMIN"),
                600000
        );

        Claims claims =
                jwtUtil.validateAndGetClaims(token);

        assertEquals(
                "admin@test.com",
                claims.getSubject()
        );

        assertEquals(
                1L,
                ((Number) claims.get("userId"))
                        .longValue()
        );

        assertEquals(
                List.of("ADMIN"),
                claims.get("roles", List.class)
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForMalformedToken() {

        assertFalse(
                jwtUtil.isTokenValid("invalid.jwt.token")
        );
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken() {

        String token = createToken(
                "user@test.com",
                10L,
                List.of("USER"),
                -1000
        );

        assertFalse(jwtUtil.isTokenValid(token));
    }

    @Test
    void extractUserId_whenClaimMissing_shouldThrowException() {

        Date now = new Date();

        String token = Jwts.builder()
                .subject("user@test.com")
                .claim("roles", List.of("USER"))
                .issuedAt(now)
                .expiration(
                        new Date(now.getTime() + 600000)
                )
                .signWith(secretKey)
                .compact();

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> jwtUtil.extractUserId(token)
                );

        assertEquals(
                "userId claim is missing from token",
                exception.getMessage()
        );
    }

    @Test
    void tokenSignedWithDifferentSecret_shouldBeInvalid() {

        String differentSecret =
                "AnotherTestJwtSecretKeyUsedOnlyForAutomatedTesting123456";

        SecretKey differentKey =
                Keys.hmacShaKeyFor(
                        differentSecret.getBytes(StandardCharsets.UTF_8)
                );

        Date now = new Date();

        String token = Jwts.builder()
                .subject("user@test.com")
                .expiration(
                        new Date(now.getTime() + 600000)
                )
                .signWith(differentKey)
                .compact();

        assertFalse(jwtUtil.isTokenValid(token));
    }
}
