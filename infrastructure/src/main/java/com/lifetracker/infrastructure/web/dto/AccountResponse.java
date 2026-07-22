package com.lifetracker.infrastructure.web.dto;

import java.util.UUID;

/** Wire response for an account, with its computed (signed) balance. Matches the OpenAPI {@code Account}. */
public record AccountResponse(UUID id, String name, String kind, String currency, MoneyDto balance) {
}
