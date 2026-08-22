package com.ecommerce.userservice.service;

import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.User;

import java.util.List;

public interface UserService {

    LoginResponse login(LoginRequest loginRequest);

    UserResponse register(RegisterRequest registerRequest);

    List<UserResponse> getAllUsers();

    public User getUserById(long id);
}
