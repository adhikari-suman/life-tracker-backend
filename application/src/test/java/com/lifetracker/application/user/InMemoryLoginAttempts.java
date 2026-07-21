package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.LoginAttempts;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory {@link LoginAttempts} fake — a list of failure instants per email, filtered by cutoff
 * on read. Assertable, no database.
 */
final class InMemoryLoginAttempts implements LoginAttempts {

    private final Map<String, List<Instant>> byEmail = new HashMap<>();

    @Override
    public List<Instant> failuresSince(Email email, Instant cutoff) {
        return byEmail.getOrDefault(email.value(), List.of()).stream()
                .filter(at -> !at.isBefore(cutoff))
                .toList();
    }

    @Override
    public void recordFailure(Email email, Instant at) {
        byEmail.computeIfAbsent(email.value(), k -> new ArrayList<>()).add(at);
    }

    @Override
    public void clearFailures(Email email) {
        byEmail.remove(email.value());
    }

    /** Total recorded failures for an email, for assertions. */
    int totalFor(String email) {
        return byEmail.getOrDefault(email, List.of()).size();
    }
}
