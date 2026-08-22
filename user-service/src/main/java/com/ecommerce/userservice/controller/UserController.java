package com.ecommerce.userservice.controller;


import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<String> getProfile(){
        return ResponseEntity.ok("Protect User Profile Endpoints...");
    }

}
