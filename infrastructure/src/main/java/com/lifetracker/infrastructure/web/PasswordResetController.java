package com.lifetracker.infrastructure.web;

import com.lifetracker.application.user.RequestPasswordReset;
import com.lifetracker.application.user.ResetPassword;
import com.lifetracker.application.user.ResetPasswordCommand;
import com.lifetracker.infrastructure.web.dto.PasswordResetConfirmRequest;
import com.lifetracker.infrastructure.web.dto.PasswordResetRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Password reset, both public: request a link by email (always 202, non-enumerating), and confirm a
 * new password with the token (204, revoking every Session). Thin — parse, call the use case. Tokens
 * are not bean-validated so a bad one maps to 400 INVALID_TOKEN, not 422.
 */
@RestController
@RequestMapping("/auth/password-reset")
class PasswordResetController {

    private final RequestPasswordReset requestPasswordReset;
    private final ResetPassword resetPassword;

    PasswordResetController(RequestPasswordReset requestPasswordReset, ResetPassword resetPassword) {
        this.requestPasswordReset = requestPasswordReset;
        this.resetPassword = resetPassword;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    void request(@RequestBody PasswordResetRequest request) {
        requestPasswordReset.execute(request.email());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirm(@RequestBody PasswordResetConfirmRequest request) {
        resetPassword.execute(new ResetPasswordCommand(request.token(), request.newPassword()));
    }
}
