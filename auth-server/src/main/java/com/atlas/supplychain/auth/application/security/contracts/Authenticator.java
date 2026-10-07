package com.atlas.supplychain.auth.application.security.contracts;

import java.util.List;

public interface Authenticator {
    AuthenticatedUser authenticate(String idUser, String password);

    record AuthenticatedUser(String idUser, String username, String name, String lastName, String idCompany, List<String> roles, List<String> authorities) {}
}