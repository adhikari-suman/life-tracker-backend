package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantRepository;

/**
 * Revokes a View Grant. Only the owner may revoke their own grant; an unknown grant, or one on
 * someone else's Book, is reported as {@link ViewGrantNotFoundException} — never revealing another
 * owner's grants.
 */
public final class RevokeView {

    private final ViewGrantRepository grants;

    public RevokeView(ViewGrantRepository grants) {
        this.grants = grants;
    }

    public void execute(RevokeViewCommand command) {
        ViewGrant grant = grants.findById(command.grantId())
                .filter(g -> g.ownerId().equals(command.ownerId()))
                .orElseThrow(ViewGrantNotFoundException::new);
        grants.deleteById(grant.id());
    }
}
