package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ViewGrantTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Email GRANTEE = new Email("viewer@example.com");

    @Test
    void identity_is_the_id() {
        ViewGrantId id = ViewGrantId.generate();
        ViewGrant a = ViewGrant.create(id, UserId.generate(), UserId.generate(), GRANTEE, T0);
        ViewGrant b = ViewGrant.create(id, UserId.generate(), UserId.generate(), GRANTEE, T0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void different_ids_are_different_grants() {
        UserId owner = UserId.generate();
        UserId grantee = UserId.generate();
        assertNotEquals(
                ViewGrant.create(ViewGrantId.generate(), owner, grantee, GRANTEE, T0),
                ViewGrant.create(ViewGrantId.generate(), owner, grantee, GRANTEE, T0));
    }
}
