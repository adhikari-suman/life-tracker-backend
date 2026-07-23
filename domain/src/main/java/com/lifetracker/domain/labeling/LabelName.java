package com.lifetracker.domain.labeling;

import java.util.Locale;
import java.util.Objects;

/**
 * A label's display name — trimmed, non-blank, and bounded. Names are compared case-insensitively
 * when checking that siblings are unique, but the casing the user typed is what is stored and shown:
 * {@code Fast Food} and {@code fast food} cannot be siblings, and whichever was created keeps its
 * capitalisation.
 */
public record LabelName(String value) {

    private static final int MAX_LENGTH = 100;

    public LabelName {
        Objects.requireNonNull(value, "value");
        value = value.trim();
        if (value.isEmpty()) {
            throw new InvalidLabelNameException("label name must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new InvalidLabelNameException("label name must be at most " + MAX_LENGTH + " characters");
        }
    }

    /** The form used to compare siblings for uniqueness. */
    public String normalized() {
        return value.toLowerCase(Locale.ROOT);
    }

    public boolean sameAs(LabelName other) {
        return other != null && normalized().equals(other.normalized());
    }
}
