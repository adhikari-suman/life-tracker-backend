package com.lifetracker.domain.sharing;

/**
 * Generates unguessable Share Link tokens. A port — cryptographic randomness is an infrastructure
 * concern.
 */
public interface ShareTokens {

    ShareToken generate();
}
