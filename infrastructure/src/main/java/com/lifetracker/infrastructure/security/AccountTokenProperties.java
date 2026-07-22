package com.lifetracker.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Lifetimes for the account one-time tokens, bound from {@code app.auth.tokens.*} (ADR-0011).
 * Verification links live longer than reset links, which are the more sensitive of the two.
 */
@ConfigurationProperties("app.auth.tokens")
public record AccountTokenProperties(
        @DefaultValue("P1D") Duration verificationTtl,
        @DefaultValue("PT1H") Duration passwordResetTtl) {
}
