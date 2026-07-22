package com.lifetracker.infrastructure.web.dto;

/** Body of {@code POST /auth/password-reset}: the email to send a reset link to (if it exists). */
public record PasswordResetRequest(String email) {
}
