package com.lifetracker.domain.token;

/** What a {@link OneTimeToken} authorizes. One token concept, two jobs (ADR-0011). */
public enum TokenPurpose {
    VERIFY_EMAIL,
    RESET_PASSWORD
}
