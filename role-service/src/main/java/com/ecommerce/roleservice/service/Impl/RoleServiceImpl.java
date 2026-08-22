package com.ecommerce.roleservice.service.Impl;

import com.ecommerce.roleservice.dto.RoleDto;
import com.ecommerce.roleservice.entity.Role;
import com.ecommerce.roleservice.exception.RoleAlreadyExistException;
import com.ecommerce.roleservice.exception.RoleNotFoundException;
import com.ecommerce.roleservice.repository.RoleRepository;
import com.ecommerce.roleservice.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository){
        this.roleRepository = roleRepository;
    }
    @Override
    public RoleDto createRole(String name) {

        if(roleRepository.existsByName(name)){
            throw new RoleAlreadyExistException("Role already Exist "+name);
        }

        Role role=new Role();
        role.setName(name.toUpperCase());

        Role saverole = roleRepository.save(role);

        return mapToDto(saverole);
    }

    private RoleDto mapToDto(Role saverole) {
        return new RoleDto(
                saverole.getId(),
                saverole.getName()
        );
    }

    @Override
    public RoleDto getRoleById(Long id) {

        Role role = roleRepository.findById(id).orElseThrow(() ->
                new RoleNotFoundException("Role Not Found With ID "+id));

        return mapToDto(role);
    }

    @Override
    public RoleDto getRoleByName(String name) {

        Role role=roleRepository.findByName(name.toUpperCase()).orElseThrow(()->
                new RoleNotFoundException("Role Not Found "+name.toUpperCase()));
        return mapToDto(role);
    }

    @Override
    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public void deleteRole(Long id) {

        if(!roleRepository.existsById(id)){
            throw new RoleNotFoundException("Role Not Found by Id" +id);
        }

        roleRepository.deleteById(id);

    }
}
