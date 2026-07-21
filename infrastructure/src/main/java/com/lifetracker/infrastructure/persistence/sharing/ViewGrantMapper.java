package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.UserId;

import java.time.ZoneOffset;

/** Converts between the domain {@link ViewGrant} and {@link ViewGrantEntity}. */
final class ViewGrantMapper {

    private ViewGrantMapper() {
    }

    static ViewGrantEntity toEntity(ViewGrant grant) {
        return new ViewGrantEntity(
                grant.id().value(),
                grant.ownerId().value(),
                grant.granteeId().value(),
                grant.granteeEmail().value(),
                grant.createdAt().atOffset(ZoneOffset.UTC));
    }

    static ViewGrant toDomain(ViewGrantEntity entity) {
        return ViewGrant.rehydrate(
                ViewGrantId.of(entity.getId()),
                UserId.of(entity.getOwnerUserId()),
                UserId.of(entity.getGranteeUserId()),
                new Email(entity.getGranteeEmail()),
                entity.getCreatedAt().toInstant());
    }
}
