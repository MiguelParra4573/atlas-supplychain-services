package com.atlas.supplychain.auth.application.security.dto;

public record ChangePasswordResponse(
        boolean changed,
        String message
) {}
