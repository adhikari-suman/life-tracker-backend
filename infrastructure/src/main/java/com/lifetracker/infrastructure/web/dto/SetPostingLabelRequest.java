package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/** Body of {@code PUT /postings/{postingId}/label}: the label to attach. */
public record SetPostingLabelRequest(UUID labelId) {
}
