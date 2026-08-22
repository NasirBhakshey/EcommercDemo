package com.ecommerce.userservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/testadmin")
    public ResponseEntity<String> testAdmin(){
        return ResponseEntity.ok("Admin Endpoint Access Successfully...");
    }
}
