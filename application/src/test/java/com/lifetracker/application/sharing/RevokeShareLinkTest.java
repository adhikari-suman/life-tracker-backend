package com.lifetracker.application.sharing;

import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RevokeShareLinkTest {

    private final InMemoryShareLinkRepository links = new InMemoryShareLinkRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final CreateShareLink createShareLink = new CreateShareLink(links, new FakeShareTokens(), clock);
    private final RevokeShareLink revokeShareLink = new RevokeShareLink(links);

    @Test
    void revoke_burns_the_link() {
        UserId owner = UserId.generate();
        createShareLink.execute(owner);

        revokeShareLink.execute(owner);

        assertTrue(links.findByOwnerId(owner).isEmpty());
    }

    @Test
    void revoke_is_idempotent_when_there_is_no_link() {
        assertDoesNotThrow(() -> revokeShareLink.execute(UserId.generate()));
    }

    @Test
    void re_sharing_after_a_revoke_mints_a_fresh_token() {
        UserId owner = UserId.generate();
        ShareToken first = createShareLink.execute(owner).shareLink().token();

        revokeShareLink.execute(owner);
        ShareToken second = createShareLink.execute(owner).shareLink().token();

        assertNotEquals(first, second);
    }
}
