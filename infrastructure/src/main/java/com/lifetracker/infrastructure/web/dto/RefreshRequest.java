package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Wire request for {@code POST /auth/refresh}: the {@code sessionId.secret} token from a prior response. */
public record RefreshRequest(@NotBlank String refreshToken) {
}
