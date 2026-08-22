package com.ecommerce.userservice.controller;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService){
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody RegisterRequest request){
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest){
        LoginResponse response = userService.login(loginRequest);
        return ResponseEntity.ok(response);
    }


}
