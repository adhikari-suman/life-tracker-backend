package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeViewTest {

    private final InMemoryViewGrantRepository grants = new InMemoryViewGrantRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final GrantView grantView = new GrantView(grants, users, clock);
    private final RevokeView revokeView = new RevokeView(grants);

    private UserId verifiedOwner() {
        User owner = User.register(UserId.generate(), new Email("owner@example.com"), new PasswordHash("hash"));
        owner.verifyEmail();
        users.save(owner);
        return owner.id();
    }

    private ViewGrant grantTo(UserId owner, String email) {
        users.save(User.register(UserId.generate(), new Email(email), new PasswordHash("hash")));
        return grantView.execute(new GrantViewCommand(owner, email));
    }

    @Test
    void owner_revokes_their_own_grant() {
        UserId owner = verifiedOwner();
        ViewGrant grant = grantTo(owner, "viewer@example.com");

        revokeView.execute(new RevokeViewCommand(owner, grant.id()));

        assertTrue(grants.findById(grant.id()).isEmpty());
    }

    @Test
    void revoking_an_unknown_grant_is_not_found() {
        assertThrows(ViewGrantNotFoundException.class,
                () -> revokeView.execute(new RevokeViewCommand(UserId.generate(), ViewGrantId.generate())));
    }

    @Test
    void revoking_someone_elses_grant_is_not_found() {
        UserId owner = verifiedOwner();
        ViewGrant grant = grantTo(owner, "viewer@example.com");

        assertThrows(ViewGrantNotFoundException.class,
                () -> revokeView.execute(new RevokeViewCommand(UserId.generate(), grant.id())));
        assertTrue(grants.findById(grant.id()).isPresent());
    }
}
