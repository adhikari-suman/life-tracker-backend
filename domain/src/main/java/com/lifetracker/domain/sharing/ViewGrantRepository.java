package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.UserId;

import java.util.List;
import java.util.Optional;

/**
 * The store of View Grants. {@code findByOwnerIdAndGranteeId} serves both the "already granted?"
 * check and the authenticated-viewer access resolution ("may this User read that owner's Book?").
 */
public interface ViewGrantRepository {

    void save(ViewGrant grant);

    Optional<ViewGrant> findById(ViewGrantId id);

    List<ViewGrant> findByOwnerId(UserId ownerId);

    Optional<ViewGrant> findByOwnerIdAndGranteeId(UserId ownerId, UserId granteeId);

    void deleteById(ViewGrantId id);
}
