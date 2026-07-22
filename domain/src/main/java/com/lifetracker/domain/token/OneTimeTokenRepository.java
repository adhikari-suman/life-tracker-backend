package com.lifetracker.domain.token;

import com.lifetracker.domain.user.UserId;

import java.util.Optional;

/**
 * The store of one-time tokens, as the domain needs it. Lookup is by hash — the presented secret is
 * hashed and matched, never compared raw. The JPA adapter lives in infrastructure.
 */
public interface OneTimeTokenRepository {

    void save(OneTimeToken token);

    Optional<OneTimeToken> findByHash(OneTimeTokenHash hash);

    /** Consume a token once used. */
    void delete(OneTimeToken token);

    /** Invalidate a User's outstanding tokens of a purpose, so only the newest link works. */
    void deleteByUserIdAndPurpose(UserId userId, TokenPurpose purpose);
}
