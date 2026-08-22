package com.ecommerce.roleservice.controller;

import com.ecommerce.roleservice.dto.RoleDto;
import com.ecommerce.roleservice.service.RoleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService){
        this.roleService = roleService;
    }

    @PostMapping
    public ResponseEntity<RoleDto> createRole(@RequestParam String name){
        RoleDto roleDto=roleService.createRole(name);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roleDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id){
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name){
        return ResponseEntity.ok(roleService.getRoleByName(name));
    }

    @GetMapping
    public ResponseEntity<List<RoleDto>> getAllRoles(){
        return ResponseEntity.ok(roleService.getAllRoles());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id){

        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();

    }
}
