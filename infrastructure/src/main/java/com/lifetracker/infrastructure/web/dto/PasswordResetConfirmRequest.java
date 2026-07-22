package com.lifetracker.infrastructure.web.dto;

/** Body of {@code POST /auth/password-reset/confirm}: the reset token and the new password. */
public record PasswordResetConfirmRequest(String token, String newPassword) {

    @Override
    public String toString() {
        return "PasswordResetConfirmRequest[token=REDACTED, newPassword=REDACTED]";
    }
}
