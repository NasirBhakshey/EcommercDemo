package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {

    @Mock
    private UserService userService;

    private AuthController authController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(userService);
    }

    @Test
    void register_shouldReturnCreatedStatusAndUserResponse() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("user@test.com");
        request.setPassword("password123");

        UserResponse userResponse =
                new UserResponse(
                        1L,
                        "Test User",
                        "user@test.com"
                );

        when(userService.register(request))
                .thenReturn(userResponse);

        ResponseEntity<UserResponse> response =
                authController.register(request);

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals(
                "Test User",
                response.getBody().getName()
        );
        assertEquals(
                "user@test.com",
                response.getBody().getEmail()
        );

        verify(userService).register(request);
    }

    @Test
    void login_shouldReturnOkStatusAndLoginResponse() {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        UserResponse userResponse =
                new UserResponse(
                        1L,
                        "Test User",
                        "user@test.com"
                );

        LoginResponse loginResponse =
                new LoginResponse(
                        "test-jwt-token",
                        "Bearer",
                        userResponse
                );

        when(userService.login(request))
                .thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response =
                authController.login(request);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());
        assertSame(loginResponse, response.getBody());

        verify(userService).login(request);
    }
}
