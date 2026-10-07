package com.atlas.supplychain.auth.application.security;

import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordResponse;
import com.atlas.supplychain.auth.application.security.dto.GenericMessageResponse;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;

public interface PasswordResetService {

    ForgotPasswordResponse forgot(ForgotPasswordRequest req);

    GenericMessageResponse reset(ResetPasswordRequest req) throws EntityNotFoundException;

}
