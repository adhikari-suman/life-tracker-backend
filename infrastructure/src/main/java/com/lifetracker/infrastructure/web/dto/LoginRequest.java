package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Wire request for {@code POST /auth/login}. {@code deviceLabel} is optional (falls back to the User-Agent). */
public record LoginRequest(@NotBlank String email, @NotBlank String password, String deviceLabel) {
}
