package com.lifetracker.infrastructure.web.dto;

/** Body of {@code POST /accounts}: name, kind (AccountKind), currency (ISO 4217). */
public record CreateAccountRequest(String name, String kind, String currency) {
}
