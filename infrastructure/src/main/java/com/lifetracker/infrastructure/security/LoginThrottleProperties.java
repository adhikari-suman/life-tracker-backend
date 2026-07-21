package com.lifetracker.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * The login brute-force policy, bound from {@code app.auth.login.*}: at most {@code maxAttempts}
 * failures per email within a trailing {@code window} (ADR-0010). Defaults apply when unset, so a
 * missing config is a sane default rather than a boot failure; the {@code LoginThrottle} it feeds
 * still rejects a nonsensical value.
 */
@ConfigurationProperties("app.auth.login")
public record LoginThrottleProperties(
        @DefaultValue("10") int maxAttempts,
        @DefaultValue("1h") Duration window) {
}
