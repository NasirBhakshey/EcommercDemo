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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RoleClient roleClient;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                jwtService,
                userRepository,
                passwordEncoder,
                roleClient
        );
    }

    @Test
    void register_shouldCreateUserWithDefaultUserRole() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("user@test.com");
        request.setPassword("password123");

        RoleDto roleDto = new RoleDto();
        roleDto.setName("USER");

        when(userRepository.existsByEmail("user@test.com"))
                .thenReturn(false);

        when(roleClient.getRoleByName("USER"))
                .thenReturn(roleDto);

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        UserResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("user@test.com", response.getEmail());

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertEquals("Test User", savedUser.getName());
        assertEquals("user@test.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(Set.of("USER"), savedUser.getRoles());

        verify(userRepository).existsByEmail("user@test.com");
        verify(roleClient).getRoleByName("USER");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_shouldThrowExceptionWhenEmailAlreadyExists() {

        RegisterRequest request = new RegisterRequest();
        request.setName("Test User");
        request.setEmail("user@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("user@test.com"))
                .thenReturn(true);

        UserAlreadyExistsException exception =
                assertThrows(
                        UserAlreadyExistsException.class,
                        () -> userService.register(request)
                );

        assertEquals(
                "Email is already Registered...",
                exception.getMessage()
        );

        verify(userRepository).existsByEmail("user@test.com");

        verifyNoInteractions(roleClient);
        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void login_shouldReturnTokenAndUserWhenCredentialsAreValid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRoles(Set.of("USER"));

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateToken(user))
                .thenReturn("test-jwt-token");

        LoginResponse response = userService.login(request);

        assertNotNull(response);

        verify(userRepository)
                .findByEmail("user@test.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encoded-password"
                );

        verify(jwtService)
                .generateToken(user);
    }

    @Test
    void login_shouldThrowExceptionWhenEmailDoesNotExist() {

        LoginRequest request = new LoginRequest();
        request.setEmail("missing@test.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("missing@test.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.login(request)
                );

        assertEquals(
                "Invalid Email or Password",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("missing@test.com");

        verifyNoInteractions(passwordEncoder);
        verifyNoInteractions(jwtService);
    }


    @Test
    void login_shouldThrowExceptionWhenPasswordIsInvalid() {

        LoginRequest request = new LoginRequest();
        request.setEmail("user@test.com");
        request.setPassword("wrong-password");

        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("user@test.com");
        user.setPassword("encoded-password");
        user.setRoles(Set.of("USER"));

        when(userRepository.findByEmail("user@test.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> userService.login(request)
                );

        assertEquals(
                "Email or Password Invalid...",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("user@test.com");

        verify(passwordEncoder)
                .matches(
                        "wrong-password",
                        "encoded-password"
                );

        verifyNoInteractions(jwtService);
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {

        User user1 = new User();
        user1.setId(1L);
        user1.setName("User One");
        user1.setEmail("user1@test.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setName("User Two");
        user2.setEmail("user2@test.com");

        when(userRepository.findAll())
                .thenReturn(List.of(user1, user2));

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(1L, result.get(0).getId());
        assertEquals("User One", result.get(0).getName());
        assertEquals("user1@test.com", result.get(0).getEmail());

        assertEquals(2L, result.get(1).getId());
        assertEquals("User Two", result.get(1).getName());
        assertEquals("user2@test.com", result.get(1).getEmail());

        verify(userRepository).findAll();
    }


    @Test
    void getUserById_shouldReturnUserWhenUserExists() {

        User user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("user@test.com");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test User", result.getName());
        assertEquals("user@test.com", result.getEmail());

        verify(userRepository).findById(1L);
    }


    @Test
    void getUserById_shouldThrowExceptionWhenUserDoesNotExist() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        UserNotFoundException exception =
                assertThrows(
                        UserNotFoundException.class,
                        () -> userService.getUserById(999L)
                );

        assertEquals(
                "User Not Found with Id 999",
                exception.getMessage()
        );

        verify(userRepository).findById(999L);
    }
}
