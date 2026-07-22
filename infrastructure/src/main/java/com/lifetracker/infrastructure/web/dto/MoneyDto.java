package com.lifetracker.infrastructure.web.dto;

/** Money on the wire: a decimal STRING amount and an ISO 4217 currency (never a JSON number). */
public record MoneyDto(String amount, String currency) {
}
