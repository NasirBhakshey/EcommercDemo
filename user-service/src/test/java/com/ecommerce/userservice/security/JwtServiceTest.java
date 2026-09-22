package com.ecommerce.userservice.security;

import com.ecommerce.userservice.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;

    private static final String TEST_SECRET =
            "ThisIsATestJwtSecretKeyOnlyForAutomatedTesting123456789";

    private static final long EXPIRATION = 600000; // 10 minutes

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                TEST_SECRET,
                EXPIRATION
        );
    }

    @Test
    void generateToken_shouldCreateValidToken() {

        User user = createUser();

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void extractEmail_shouldReturnUserEmail() {

        User user = createUser();

        String token = jwtService.generateToken(user);

        String email = jwtService.extractEmail(token);

        assertEquals(
                "user@test.com",
                email
        );
    }

    @Test
    void getClaims_shouldContainUserInformation() {

        User user = createUser();

        String token = jwtService.generateToken(user);

        Claims claims = jwtService.getClaims(token);

        assertEquals(
                "user@test.com",
                claims.getSubject()
        );

        assertEquals(
                "user@test.com",
                claims.get("email", String.class)
        );

        /*
         * JSON/JWT numeric values may be deserialized as Integer
         * rather than Long, so Number is safer here.
         */
        Number userId = claims.get("userId", Number.class);

        assertNotNull(userId);
        assertEquals(1L, userId.longValue());

        assertNotNull(claims.get("roles"));
    }

    @Test
    void isTokenValid_shouldReturnFalseForInvalidToken() {

        boolean valid = jwtService.isTokenValid(
                "this-is-not-a-valid-jwt"
        );

        assertFalse(valid);
    }

    @Test
    void isTokenValid_shouldReturnFalseForExpiredToken()
            throws InterruptedException {

        JwtService shortLivedJwtService =
                new JwtService(
                        TEST_SECRET,
                        1
                );

        User user = createUser();

        String token =
                shortLivedJwtService.generateToken(user);

        Thread.sleep(10);

        assertFalse(
                shortLivedJwtService.isTokenValid(token)
        );
    }

    private User createUser() {

        User user = new User();

        user.setId(1L);
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRoles(Set.of("USER"));

        return user;
    }
}
