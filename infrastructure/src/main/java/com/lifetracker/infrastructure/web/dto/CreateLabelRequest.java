package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Body of {@code POST /labels}: a name, and the parent to nest under (null for a root label).
 *
 * <p>{@code name} is the only {@code required} field, so it alone is {@code @NotBlank} — an absent
 * {@code parentId} is not an omission but the way you say "root".
 */
public record CreateLabelRequest(@NotBlank String name, UUID parentId) {
}
