package com.atlas.supplychain.auth.application.security.contracts;

import java.util.List;

public record AccessTokenClaims(String username, String idCompany, List<String> roles, List<String> authorities) {
}
