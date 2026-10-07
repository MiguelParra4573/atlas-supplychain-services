package com.atlas.supplychain.auth.application.security.dto;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank String usuario, @NotBlank String idCompany) {}