package com.atlas.supplychain.auth.application.security.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    @NotBlank
    private String usuario;
    @NotBlank
    private String contrasena;
}