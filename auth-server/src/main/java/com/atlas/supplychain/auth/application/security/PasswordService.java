package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;

public interface PasswordService {

    void changeMyPassword(String idUser, String idCompany, ChangePasswordRequest req) throws EntityNotFoundException;

}
