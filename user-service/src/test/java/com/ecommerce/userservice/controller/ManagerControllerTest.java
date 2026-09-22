package com.ecommerce.userservice.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ManagerControllerTest {

    @Test
    void testManager_shouldReturnSuccessMessage() {

        ManagerController managerController =
                new ManagerController();

        ResponseEntity<String> response =
                managerController.testManager();

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertEquals(
                "Manager endpoint accessed successfully...",
                response.getBody()
        );
    }
}
