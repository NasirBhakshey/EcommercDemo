package com.ecommerce.roleservice.service;

import com.ecommerce.roleservice.dto.RoleDto;
import com.ecommerce.roleservice.entity.Role;
import com.ecommerce.roleservice.exception.RoleAlreadyExistException;
import com.ecommerce.roleservice.exception.RoleNotFoundException;
import com.ecommerce.roleservice.repository.RoleRepository;
import com.ecommerce.roleservice.service.Impl.RoleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    private RoleServiceImpl roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleServiceImpl(roleRepository);
    }

    @Test
    void createRole_shouldCreateRoleSuccessfully() {

        when(roleRepository.existsByName("admin"))
                .thenReturn(false);

        when(roleRepository.save(any(Role.class)))
                .thenAnswer(invocation -> {
                    Role role = invocation.getArgument(0);
                    role.setId(1L);
                    return role;
                });

        RoleDto result = roleService.createRole("admin");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ADMIN", result.getName());

        verify(roleRepository).existsByName("admin");

        verify(roleRepository).save(argThat(role ->
                "ADMIN".equals(role.getName())
        ));
    }

    @Test
    void createRole_whenRoleAlreadyExists_shouldThrowException() {

        when(roleRepository.existsByName("ADMIN"))
                .thenReturn(true);

        assertThrows(
                RoleAlreadyExistException.class,
                () -> roleService.createRole("ADMIN")
        );

        verify(roleRepository).existsByName("ADMIN");
        verify(roleRepository, never()).save(any());
    }

    @Test
    void getRoleById_shouldReturnRole() {

        Role role = new Role();
        role.setId(1L);
        role.setName("ADMIN");

        when(roleRepository.findById(1L))
                .thenReturn(Optional.of(role));

        RoleDto result = roleService.getRoleById(1L);

        assertEquals(1L, result.getId());
        assertEquals("ADMIN", result.getName());

        verify(roleRepository).findById(1L);
    }

    @Test
    void getRoleById_whenRoleDoesNotExist_shouldThrowException() {

        when(roleRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RoleNotFoundException.class,
                () -> roleService.getRoleById(99L)
        );

        verify(roleRepository).findById(99L);
    }

    @Test
    void getRoleByName_shouldConvertNameToUppercaseAndReturnRole() {

        Role role = new Role();
        role.setId(2L);
        role.setName("MANAGER");

        when(roleRepository.findByName("MANAGER"))
                .thenReturn(Optional.of(role));

        RoleDto result = roleService.getRoleByName("manager");

        assertEquals(2L, result.getId());
        assertEquals("MANAGER", result.getName());

        verify(roleRepository).findByName("MANAGER");
    }

    @Test
    void getRoleByName_whenRoleDoesNotExist_shouldThrowException() {

        when(roleRepository.findByName("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                RoleNotFoundException.class,
                () -> roleService.getRoleByName("unknown")
        );

        verify(roleRepository).findByName("UNKNOWN");
    }

    @Test
    void getAllRoles_shouldReturnAllRoles() {

        Role admin = new Role();
        admin.setId(1L);
        admin.setName("ADMIN");

        Role user = new Role();
        user.setId(2L);
        user.setName("USER");

        when(roleRepository.findAll())
                .thenReturn(List.of(admin, user));

        List<RoleDto> result = roleService.getAllRoles();

        assertEquals(2, result.size());

        assertEquals("ADMIN", result.get(0).getName());
        assertEquals("USER", result.get(1).getName());

        verify(roleRepository).findAll();
    }

    @Test
    void deleteRole_shouldDeleteExistingRole() {

        when(roleRepository.existsById(1L))
                .thenReturn(true);

        roleService.deleteRole(1L);

        verify(roleRepository).existsById(1L);
        verify(roleRepository).deleteById(1L);
    }

    @Test
    void deleteRole_whenRoleDoesNotExist_shouldThrowException() {

        when(roleRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(
                RoleNotFoundException.class,
                () -> roleService.deleteRole(99L)
        );

        verify(roleRepository).existsById(99L);
        verify(roleRepository, never()).deleteById(anyLong());
    }
}
