package com.lifetracker.application.sharing;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateShareLinkTest {

    private final InMemoryShareLinkRepository links = new InMemoryShareLinkRepository();
    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final CreateShareLink createShareLink = new CreateShareLink(links, new FakeShareTokens(), clock);

    @Test
    void mints_a_new_link_the_first_time() {
        UserId owner = UserId.generate();

        CreateShareLinkResult result = createShareLink.execute(owner);

        assertTrue(result.created());
        assertEquals(owner, result.shareLink().ownerId());
        assertTrue(links.findByOwnerId(owner).isPresent());
    }

    @Test
    void returns_the_same_link_the_second_time() {
        UserId owner = UserId.generate();

        CreateShareLinkResult first = createShareLink.execute(owner);
        CreateShareLinkResult second = createShareLink.execute(owner);

        assertFalse(second.created());
        assertEquals(first.shareLink().token(), second.shareLink().token());
    }
}
