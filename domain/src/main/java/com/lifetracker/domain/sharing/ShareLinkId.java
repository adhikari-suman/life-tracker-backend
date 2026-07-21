package com.lifetracker.domain.sharing;

import java.util.Objects;
import java.util.UUID;

/** The identity of a {@link ShareLink}. */
public record ShareLinkId(UUID value) {

    public ShareLinkId {
        Objects.requireNonNull(value, "value");
    }

    public static ShareLinkId generate() {
        return new ShareLinkId(UUID.randomUUID());
    }

    public static ShareLinkId of(UUID value) {
        return new ShareLinkId(value);
    }
}
