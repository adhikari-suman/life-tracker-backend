package com.lifetracker.infrastructure.persistence.token;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenId;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.UserId;

/** Converts between the domain {@link OneTimeToken} and {@link OneTimeTokenEntity}. */
final class OneTimeTokenMapper {

    private OneTimeTokenMapper() {
    }

    static OneTimeTokenEntity toEntity(OneTimeToken token) {
        return new OneTimeTokenEntity(
                token.id().value(),
                token.userId().value(),
                token.tokenHash().value(),
                token.purpose().name(),
                token.expiresAt(),
                token.createdAt());
    }

    static OneTimeToken toDomain(OneTimeTokenEntity entity) {
        return OneTimeToken.rehydrate(
                OneTimeTokenId.of(entity.getId()),
                UserId.of(entity.getUserId()),
                new OneTimeTokenHash(entity.getTokenHash()),
                TokenPurpose.valueOf(entity.getPurpose()),
                entity.getExpiresAt(),
                entity.getCreatedAt());
    }
}
