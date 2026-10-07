package com.atlas.supplychain.auth.infrastructure.maintenance.authorization.in.rest.auth;

import com.atlas.supplychain.auth.application.security.dto.ChangePasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ChangePasswordResponse;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.ForgotPasswordResponse;
import com.atlas.supplychain.auth.application.security.dto.GenericMessageResponse;
import com.atlas.supplychain.auth.application.security.dto.LoginRequest;
import com.atlas.supplychain.auth.application.security.dto.LogoutRequest;
import com.atlas.supplychain.auth.application.security.dto.RefreshRequest;
import com.atlas.supplychain.auth.application.security.dto.ResetPasswordRequest;
import com.atlas.supplychain.auth.application.security.dto.TokenResponse;
import com.atlas.supplychain.auth.application.security.AuthenticationService;
import com.atlas.supplychain.auth.application.security.PasswordResetService;
import com.atlas.supplychain.auth.application.security.PasswordService;
import com.atlas.supplychain.auth.domain.common.exceptions.EntityNotFoundException;
import com.atlas.supplychain.auth.infrastructure.config.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authService;
    private final PasswordService passwordService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req);
    }

    @PostMapping("/logout")
    public void logout(@Valid @RequestBody LogoutRequest req) {
        authService.logout(req);
    }

    @PostMapping("/change-password")
    public ChangePasswordResponse changePassword(
            @AuthenticationPrincipal CustomUserDetails user,
            @Valid @RequestBody ChangePasswordRequest req) throws EntityNotFoundException {
        if (user == null) throw new BadCredentialsException("No autenticado");
        passwordService.changeMyPassword(user.getUsername(), user.getIdCompany(), req);
        return new ChangePasswordResponse(true, "Contraseña actualizada. Se cerraron las demás sesiones.");
    }

    @PostMapping("/password/forgot")
    public ForgotPasswordResponse forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        return passwordResetService.forgot(req);
    }

    @PostMapping("/password/reset")
    public GenericMessageResponse reset(@Valid @RequestBody ResetPasswordRequest req) throws EntityNotFoundException {
        return passwordResetService.reset(req);
    }


}