package com.ecommerce.userservice.client;

import com.ecommerce.common.dto.RoleDto;
import com.ecommerce.userservice.entity.User;
import com.ecommerce.userservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleClient roleClient;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleClient roleClient){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleClient = roleClient;
    }
    @Override
    public void run(String... args) throws Exception {

        createUserIfNotExist(
                "Admin",
                "admin@gmail.com",
                "admin@1234",
                "ADMIN");
        createUserIfNotExist(
                "User",
                "user@gmail.com",
                "user@1234",
                "USER");
        createUserIfNotExist(
                "MANAGER",
                "manager@gmail.com",
                "manager@1234",
                "MANAGER");

    }

    private void createUserIfNotExist(String name,
                                      String email,
                                      String password,
                                      String role){

        if(userRepository.existsByEmail(email)){
            return;
        }

        RoleDto roleDto= roleClient.getRoleByName(role);

        User user=new User();

        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(Set.of(roleDto.getName()));

        userRepository.save(user);

    }
}
