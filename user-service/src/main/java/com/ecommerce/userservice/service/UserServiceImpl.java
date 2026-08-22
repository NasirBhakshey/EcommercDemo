package com.ecommerce.userservice.service;

import com.ecommerce.common.dto.RoleDto;
import com.ecommerce.userservice.client.RoleClient;
import com.ecommerce.userservice.dto.LoginRequest;
import com.ecommerce.userservice.dto.LoginResponse;
import com.ecommerce.userservice.dto.RegisterRequest;
import com.ecommerce.userservice.dto.UserResponse;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.exception.UserAlreadyExistsException;
import com.ecommerce.userservice.exception.UserNotFoundException;
import com.ecommerce.userservice.repository.UserRepository;
import com.ecommerce.userservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserServiceImpl implements UserService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RoleClient roleClient;

    public UserServiceImpl(
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RoleClient roleClient) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.roleClient = roleClient;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user=userRepository.findByEmail(loginRequest.getEmail()).orElseThrow(() ->
                new RuntimeException("Invalid Email or Password"));

        if (!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())){
            throw new RuntimeException("Email or Password Invalid...");
        }

        UserResponse userResponse = new UserResponse(user.getId(),user.getName(),user.getEmail());
        String token = jwtService.generateToken(user);

        return new LoginResponse(
                token,
                "Bearer",
                userResponse);
    }

    @Override
    public UserResponse register(RegisterRequest registerRequest) {
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new UserAlreadyExistsException("Email is already Registered...");
        }

        RoleDto roleDto= roleClient.getRoleByName("USER");

        User user=new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setName(registerRequest.getName());
        user.setRoles(Set.of(roleDto.getName()));
        User Saveuser=userRepository.save(user);

        return new UserResponse(Saveuser.getId(),
                Saveuser.getName(),
                Saveuser.getEmail());
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserResponse(
                        user.getId(),
                        user.getName(),
                        user.getEmail()
                )).toList();
    }

    @Override
    public User getUserById(long id) {
        return userRepository.findById(id).orElseThrow(()->
                new UserNotFoundException("User Not Found with Id " +id));
    }


}
