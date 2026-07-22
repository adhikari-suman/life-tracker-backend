package com.lifetracker.infrastructure.web.dto;

/** Body of {@code POST /auth/verify-email}: the token from the emailed link. */
public record VerifyEmailRequest(String token) {
}
