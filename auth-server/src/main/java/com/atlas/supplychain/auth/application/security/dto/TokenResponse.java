package com.atlas.supplychain.auth.application.security.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String tokenType;
    private String accessToken;
    private long expiresIn;
    private String refreshToken;
    private long refreshExpiresIn;
    private UserInfo user;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserInfo {
        private String usuario;
        private String name;
        private String lastName;
        private String idCompany;
        private List<String> roles;
        private List<String> authorities;
    }

}