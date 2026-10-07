package com.atlas.supplychain.auth.application.security.contracts;

import java.util.List;

public interface UserIdentityProvider {
    UserIdentity load(String idUser);

    record UserIdentity(String idUser, String username, String name, String lastName, String idCompany, List<String> roles, List<String> authorities) {
    }
}