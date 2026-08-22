package com.ecommerce.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class ManagerController {

    @GetMapping("/test")
    public ResponseEntity<String> testManager(){
        return ResponseEntity.ok("Manager endpoint accessed successfully...");
    }
}
