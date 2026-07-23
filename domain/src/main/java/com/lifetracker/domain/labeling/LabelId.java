package com.lifetracker.domain.labeling;

import java.util.Objects;
import java.util.UUID;

/** The identity of a {@link Label}. A value object wrapping a {@link UUID}. */
public record LabelId(UUID value) {

    public LabelId {
        Objects.requireNonNull(value, "value");
    }

    public static LabelId generate() {
        return new LabelId(UUID.randomUUID());
    }

    public static LabelId of(UUID value) {
        return new LabelId(value);
    }
}
