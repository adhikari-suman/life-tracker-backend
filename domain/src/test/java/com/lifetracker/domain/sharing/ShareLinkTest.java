package com.lifetracker.domain.sharing;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShareLinkTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void identity_is_the_id_not_the_owner_or_token() {
        ShareLinkId id = ShareLinkId.generate();
        ShareLink a = ShareLink.create(id, UserId.generate(), new ShareToken("token-a"), T0);
        ShareLink b = ShareLink.create(id, UserId.generate(), new ShareToken("token-b"), T0);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void different_ids_are_different_links() {
        UserId owner = UserId.generate();
        assertNotEquals(
                ShareLink.create(ShareLinkId.generate(), owner, new ShareToken("t"), T0),
                ShareLink.create(ShareLinkId.generate(), owner, new ShareToken("t"), T0));
    }

    @Test
    void token_is_redacted_in_toString() {
        ShareToken token = new ShareToken("super-secret-link-token");
        assertFalse(token.toString().contains("super-secret"));
    }
}
