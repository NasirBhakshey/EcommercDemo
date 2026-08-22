package com.ecommerce.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;
    @NotBlank
    @Size(min = 8,max = 100)
    private String password;
    @NotBlank
    private String name;
}
