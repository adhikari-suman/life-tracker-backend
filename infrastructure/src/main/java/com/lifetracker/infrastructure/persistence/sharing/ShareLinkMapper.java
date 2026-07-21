package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.sharing.ShareLink;
import com.lifetracker.domain.sharing.ShareLinkId;
import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.user.UserId;

import java.time.ZoneOffset;

/** Converts between the domain {@link ShareLink} and {@link ShareLinkEntity}. */
final class ShareLinkMapper {

    private ShareLinkMapper() {
    }

    static ShareLinkEntity toEntity(ShareLink link) {
        return new ShareLinkEntity(
                link.id().value(),
                link.ownerId().value(),
                link.token().value(),
                link.createdAt().atOffset(ZoneOffset.UTC));
    }

    static ShareLink toDomain(ShareLinkEntity entity) {
        return ShareLink.rehydrate(
                ShareLinkId.of(entity.getId()),
                UserId.of(entity.getOwnerUserId()),
                new ShareToken(entity.getToken()),
                entity.getCreatedAt().toInstant());
    }
}
