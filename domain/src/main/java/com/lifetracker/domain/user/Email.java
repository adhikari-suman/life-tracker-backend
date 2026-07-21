package com.lifetracker.domain.user;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A User's email address, and their login identifier. Normalized to lower case and trimmed so
 * uniqueness and lookup are case-insensitive — {@code "Sam@x.com"} and {@code "sam@x.com"} are
 * one User, not two.
 *
 * <p>The check is a structural sanity check, not proof of deliverability — that is what a
 * verification email (deferred, see ADR-0007) would be for.
 */
public record Email(String value) {

    // Deliberately permissive: one @, non-empty local and domain parts, and a dot in the domain.
    private static final Pattern SHAPE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public Email {
        Objects.requireNonNull(value, "value");
        value = value.trim().toLowerCase();
        if (!SHAPE.matcher(value).matches()) {
            throw new InvalidEmailException(value);
        }
    }
}
