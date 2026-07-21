package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/** Wire request for {@code POST /me/view-grants}: the email of an already-registered User. */
public record GrantViewRequest(@NotBlank String email) {
}
