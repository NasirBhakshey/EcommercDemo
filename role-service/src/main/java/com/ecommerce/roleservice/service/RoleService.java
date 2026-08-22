package com.ecommerce.roleservice.service;

import com.ecommerce.roleservice.dto.RoleDto;

import java.util.List;

public interface RoleService {

    RoleDto createRole(String name);

    RoleDto getRoleById(Long id);

    RoleDto getRoleByName(String name);

    List<RoleDto> getAllRoles();

    void deleteRole(Long id);
}
