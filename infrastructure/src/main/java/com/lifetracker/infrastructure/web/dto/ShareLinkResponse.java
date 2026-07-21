package com.lifetracker.infrastructure.web.dto;

import java.time.OffsetDateTime;

/** Wire response for the Share Link. Matches the OpenAPI {@code ShareLink} schema. */
public record ShareLinkResponse(String url, OffsetDateTime createdAt) {
}
