package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of {@code POST /accounts}: name, kind (AccountKind), currency (ISO 4217).
 *
 * <p>All three are {@code required} in the spec, so all three are {@code @NotBlank}: a body that
 * parses and then omits one is 422 VALIDATION, never a 500. Without this a missing {@code name}
 * reached the domain as a null and threw, while a missing {@code kind} happened to answer 422
 * because the enum failed to deserialize — the same client mistake getting two different answers
 * depending on which field it landed on.
 */
public record CreateAccountRequest(@NotBlank String name, @NotBlank String kind,
                                   @NotBlank String currency) {
}
