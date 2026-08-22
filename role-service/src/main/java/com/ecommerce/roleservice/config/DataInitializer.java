package com.ecommerce.roleservice.config;

import com.ecommerce.roleservice.entity.Role;
import com.ecommerce.roleservice.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }


    @Override
    public void run(String... args) throws Exception {

        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("MANAGER");
        createRoleIfNotExists("USER");

    }

    private void createRoleIfNotExists(String roleName){
        if (!roleRepository.existsByName(roleName)){
            Role role= new Role();
            role.setName(roleName);

            roleRepository.save(role);
        }
    }
}
