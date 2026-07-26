package com.lifetracker.infrastructure.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Body of {@code PUT /postings/{postingId}/label}: the label to attach.
 *
 * <p>{@code labelId} is {@code required} and {@code @NotNull} with it. Clearing a label is DELETE
 * on this sub-resource, never a PUT of nothing, so an absent id here is a malformed request rather
 * than a quiet way to ask for removal.
 */
public record SetPostingLabelRequest(@NotNull UUID labelId) {
}
