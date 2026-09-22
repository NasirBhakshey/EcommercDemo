package com.ecommerce.roleservice.controller;

import com.ecommerce.roleservice.dto.RoleDto;
import com.ecommerce.roleservice.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RoleControllerTest {

    @Mock
    private RoleService roleService;

    private RoleController roleController;

    @BeforeEach
    void setUp() {
        roleController = new RoleController(roleService);
    }

    @Test
    void createRole_shouldReturnCreated() {

        RoleDto roleDto = new RoleDto(1L, "ADMIN");

        when(roleService.createRole("ADMIN"))
                .thenReturn(roleDto);

        ResponseEntity<RoleDto> response =
                roleController.createRole("ADMIN");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("ADMIN", response.getBody().getName());

        verify(roleService).createRole("ADMIN");
    }

    @Test
    void getRoleById_shouldReturnRole() {

        RoleDto roleDto = new RoleDto(1L, "ADMIN");

        when(roleService.getRoleById(1L))
                .thenReturn(roleDto);

        ResponseEntity<RoleDto> response =
                roleController.getRoleById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().getId());
        assertEquals("ADMIN", response.getBody().getName());

        verify(roleService).getRoleById(1L);
    }

    @Test
    void getRoleByName_shouldReturnRole() {

        RoleDto roleDto = new RoleDto(2L, "MANAGER");

        when(roleService.getRoleByName("MANAGER"))
                .thenReturn(roleDto);

        ResponseEntity<RoleDto> response =
                roleController.getRoleByName("MANAGER");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("MANAGER", response.getBody().getName());

        verify(roleService).getRoleByName("MANAGER");
    }

    @Test
    void getAllRoles_shouldReturnAllRoles() {

        List<RoleDto> roles = List.of(
                new RoleDto(1L, "ADMIN"),
                new RoleDto(2L, "USER"),
                new RoleDto(3L, "MANAGER")
        );

        when(roleService.getAllRoles())
                .thenReturn(roles);

        ResponseEntity<List<RoleDto>> response =
                roleController.getAllRoles();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals("ADMIN", response.getBody().get(0).getName());
        assertEquals("USER", response.getBody().get(1).getName());
        assertEquals("MANAGER", response.getBody().get(2).getName());

        verify(roleService).getAllRoles();
    }

    @Test
    void deleteRole_shouldReturnNoContent() {

        doNothing()
                .when(roleService)
                .deleteRole(1L);

        ResponseEntity<Void> response =
                roleController.deleteRole(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());

        verify(roleService).deleteRole(1L);
    }
}
