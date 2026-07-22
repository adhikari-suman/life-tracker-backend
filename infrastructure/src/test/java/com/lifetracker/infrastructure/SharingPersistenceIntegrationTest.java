package com.lifetracker.infrastructure;

import com.lifetracker.application.sharing.CreateShareLink;
import com.lifetracker.application.sharing.CreateShareLinkResult;
import com.lifetracker.application.sharing.GrantView;
import com.lifetracker.application.sharing.GrantViewCommand;
import com.lifetracker.application.sharing.RevokeShareLink;
import com.lifetracker.application.sharing.RevokeView;
import com.lifetracker.application.sharing.RevokeViewCommand;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.application.user.RegisterUserCommand;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;
import com.lifetracker.infrastructure.persistence.sharing.ShareLinkQueryService;
import com.lifetracker.infrastructure.persistence.sharing.ViewGrantQueryService;
import com.lifetracker.infrastructure.persistence.sharing.ViewGrantView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sharing persistence against real Postgres. The context booting proves ShareLinkEntity /
 * ViewGrantEntity match the 003 / 004 migrations (drift check); the round-trips prove create,
 * anonymous token resolution, list, and burn/revoke.
 */
class SharingPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    CreateShareLink createShareLink;

    @Autowired
    RevokeShareLink revokeShareLink;

    @Autowired
    GrantView grantView;

    @Autowired
    RevokeView revokeView;

    @Autowired
    ShareLinkRepository shareLinks;

    @Autowired
    ShareLinkQueryService shareLinkQuery;

    @Autowired
    ViewGrantQueryService viewGrantQuery;

    @Autowired
    UserRepository users;

    /** Register an owner and mark their email verified — sharing is gated on verification (ADR-0011). */
    private UserId verifiedOwner(String email) {
        UserId id = registerUser.execute(new RegisterUserCommand(email, "correct horse battery"));
        User user = users.findById(id).orElseThrow();
        user.verifyEmail();
        users.save(user);
        return id;
    }

    @Test
    void share_link_round_trips_and_resolves_by_token() {
        UserId owner = verifiedOwner("share-owner@example.com");

        CreateShareLinkResult result = createShareLink.execute(owner);
        assertTrue(result.created());

        // Anonymous lookup: a presented token resolves to the owner whose Book it opens.
        ShareToken token = result.shareLink().token();
        assertEquals(owner, shareLinks.findByToken(token).orElseThrow().ownerId());
        assertTrue(shareLinkQuery.findByOwner(owner).isPresent());

        revokeShareLink.execute(owner);
        assertTrue(shareLinkQuery.findByOwner(owner).isEmpty());
    }

    @Test
    void view_grant_round_trips_and_lists() {
        UserId owner = verifiedOwner("grant-owner@example.com");
        registerUser.execute(new RegisterUserCommand("grant-viewer@example.com", "correct horse battery"));

        ViewGrant grant = grantView.execute(new GrantViewCommand(owner, "grant-viewer@example.com"));

        List<ViewGrantView> grants = viewGrantQuery.findByOwner(owner);
        assertEquals(1, grants.size());
        assertEquals("grant-viewer@example.com", grants.get(0).granteeEmail());

        revokeView.execute(new RevokeViewCommand(owner, grant.id()));
        assertTrue(viewGrantQuery.findByOwner(owner).isEmpty());
    }
}
