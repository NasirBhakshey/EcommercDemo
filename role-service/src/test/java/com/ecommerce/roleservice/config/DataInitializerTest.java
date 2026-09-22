package com.ecommerce.roleservice.config;

import com.ecommerce.roleservice.entity.Role;
import com.ecommerce.roleservice.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DataInitializerTest {

    @Mock
    private RoleRepository roleRepository;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer(roleRepository);
    }

    @Test
    void run_whenRolesDoNotExist_shouldCreateDefaultRoles() throws Exception {

        when(roleRepository.existsByName(anyString()))
                .thenReturn(false);

        dataInitializer.run();

        verify(roleRepository).existsByName("ADMIN");
        verify(roleRepository).existsByName("MANAGER");
        verify(roleRepository).existsByName("USER");

        verify(roleRepository).save(argThat(role ->
                "ADMIN".equals(role.getName())
        ));

        verify(roleRepository).save(argThat(role ->
                "MANAGER".equals(role.getName())
        ));

        verify(roleRepository).save(argThat(role ->
                "USER".equals(role.getName())
        ));

        verify(roleRepository, times(3)).save(any(Role.class));
    }

    @Test
    void run_whenRolesAlreadyExist_shouldNotCreateRoles() throws Exception {

        when(roleRepository.existsByName(anyString()))
                .thenReturn(true);

        dataInitializer.run();

        verify(roleRepository).existsByName("ADMIN");
        verify(roleRepository).existsByName("MANAGER");
        verify(roleRepository).existsByName("USER");

        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    void run_whenOnlySomeRolesExist_shouldCreateOnlyMissingRoles() throws Exception {

        when(roleRepository.existsByName("ADMIN"))
                .thenReturn(true);

        when(roleRepository.existsByName("MANAGER"))
                .thenReturn(false);

        when(roleRepository.existsByName("USER"))
                .thenReturn(true);

        dataInitializer.run();

        verify(roleRepository, times(1)).save(any(Role.class));

        verify(roleRepository).save(argThat(role ->
                "MANAGER".equals(role.getName())
        ));
    }
}
