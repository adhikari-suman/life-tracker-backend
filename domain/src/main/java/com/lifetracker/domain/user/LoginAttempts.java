package com.lifetracker.domain.user;

import java.time.Instant;
import java.util.List;

/**
 * Records and counts failed login attempts, keyed by email — an unknown email is counted exactly
 * like a wrong password, so a lockout reveals nothing about which emails exist (ADR-0010). The port
 * {@link com.lifetracker.application.user.Authenticate} leans on for brute-force defence; the
 * adapter lives in infrastructure.
 */
public interface LoginAttempts {

    /** Timestamps of this email's failures at or after {@code cutoff}, in no particular order. */
    List<Instant> failuresSince(Email email, Instant cutoff);

    /** Record one failed attempt against this email. */
    void recordFailure(Email email, Instant at);

    /** Forget this email's failures — called after a successful login. */
    void clearFailures(Email email);
}
