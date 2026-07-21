package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Wire request for {@code POST /auth/register}. Deeper rules (email shape, password length) are the domain's. */
public record RegisterRequest(@NotBlank String email, @NotBlank String password) {
}
