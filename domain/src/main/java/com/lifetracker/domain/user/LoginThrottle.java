package com.lifetracker.domain.user;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The login brute-force policy: at most {@code maxAttempts} failed logins are tolerated for one
 * email within a trailing {@code window} (ADR-0010, keyed by email so a lockout never reveals
 * whether the email exists). A pure value — it counts and computes over failure timestamps handed
 * in from {@link LoginAttempts}; it stores nothing and reads no clock of its own.
 */
public record LoginThrottle(int maxAttempts, Duration window) {

    public LoginThrottle {
        Objects.requireNonNull(window, "window");
        if (maxAttempts < 1) {
            throw new InvalidLoginThrottleException("maxAttempts must be at least 1, was " + maxAttempts);
        }
        if (window.isZero() || window.isNegative()) {
            throw new InvalidLoginThrottleException("window must be positive, was " + window);
        }
    }

    /** True once the trailing window already holds {@code maxAttempts} failures. */
    public boolean isLockedOut(List<Instant> recentFailures) {
        return recentFailures.size() >= maxAttempts;
    }

    /**
     * How long until the caller may try again: the moment the oldest failure that keeps the count at
     * or above the limit ages out of the window. With exactly {@code maxAttempts} failures that is
     * the oldest one plus the window; with more, it is the (n − maxAttempts + 1)-th oldest, since
     * that many must expire before the count drops below the limit. {@link Duration#ZERO} when not
     * currently locked out, or when the unlock moment has already passed.
     */
    public Duration retryAfter(List<Instant> recentFailures, Instant now) {
        if (recentFailures.size() < maxAttempts) {
            return Duration.ZERO;
        }
        List<Instant> ordered = recentFailures.stream().sorted().toList();
        Instant unlockAt = ordered.get(ordered.size() - maxAttempts).plus(window);
        Duration remaining = Duration.between(now, unlockAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }
}
