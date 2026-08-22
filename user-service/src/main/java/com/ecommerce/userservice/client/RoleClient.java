package com.ecommerce.userservice.client;

import com.ecommerce.common.dto.RoleDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "role-service")
public interface RoleClient {

    @GetMapping("/roles/name/{name}")
    RoleDto getRoleByName(@PathVariable("name") String  name);
}
