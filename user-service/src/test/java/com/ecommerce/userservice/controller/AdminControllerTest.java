package com.ecommerce.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AdminControllerTest {

    @Test
    void testAdmin_shouldReturnSuccessMessage() {

        AdminController adminController =
                new AdminController();

        ResponseEntity<String> response =
                adminController.testAdmin();

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                "Admin Endpoint Access Successfully...",
                response.getBody()
        );
    }
}
