package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrantViewTest {

    private final InMemoryViewGrantRepository grants = new InMemoryViewGrantRepository();
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final GrantView grantView = new GrantView(grants, users, clock);

    private User register(String email) {
        User user = User.register(UserId.generate(), new Email(email), new PasswordHash("hash"));
        users.save(user);
        return user;
    }

    private UserId verifiedOwner() {
        User owner = register("owner@example.com");
        owner.verifyEmail();
        users.save(owner);
        return owner.id();
    }

    @Test
    void grants_view_to_an_existing_user() {
        UserId owner = verifiedOwner();
        User grantee = register("viewer@example.com");

        ViewGrant grant = grantView.execute(new GrantViewCommand(owner, "viewer@example.com"));

        assertEquals(owner, grant.ownerId());
        assertEquals(grantee.id(), grant.granteeId());
        assertEquals(new Email("viewer@example.com"), grant.granteeEmail());
    }

    @Test
    void rejects_an_unknown_email() {
        UserId owner = verifiedOwner();
        assertThrows(GranteeNotFoundException.class,
                () -> grantView.execute(new GrantViewCommand(owner, "nobody@example.com")));
    }

    @Test
    void rejects_a_duplicate_grant() {
        UserId owner = verifiedOwner();
        register("viewer@example.com");
        grantView.execute(new GrantViewCommand(owner, "viewer@example.com"));

        assertThrows(ViewGrantAlreadyExistsException.class,
                () -> grantView.execute(new GrantViewCommand(owner, "viewer@example.com")));
    }

    @Test
    void rejects_sharing_with_yourself() {
        User me = register("me@example.com");
        me.verifyEmail();
        users.save(me);
        assertThrows(CannotShareWithYourselfException.class,
                () -> grantView.execute(new GrantViewCommand(me.id(), "me@example.com")));
    }

    @Test
    void rejects_a_malformed_email() {
        UserId owner = verifiedOwner();
        assertThrows(InvalidEmailException.class,
                () -> grantView.execute(new GrantViewCommand(owner, "not-an-email")));
    }

    @Test
    void rejects_an_unverified_owner() {
        User owner = register("unverified-owner@example.com"); // never verified
        register("viewer@example.com");
        assertThrows(EmailNotVerifiedException.class,
                () -> grantView.execute(new GrantViewCommand(owner.id(), "viewer@example.com")));
    }
}
