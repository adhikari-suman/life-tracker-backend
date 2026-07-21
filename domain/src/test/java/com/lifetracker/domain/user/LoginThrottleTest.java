package com.lifetracker.domain.user;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sliding-window brute-force policy, in isolation — no clock, no database. */
class LoginThrottleTest {

    private static final Instant T0 = Instant.parse("2026-07-21T12:00:00Z");
    private final LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(15));

    @Test
    void below_the_limit_is_not_locked_out() {
        assertFalse(throttle.isLockedOut(List.of(T0, T0.plusSeconds(1))));
    }

    @Test
    void at_the_limit_is_locked_out() {
        assertTrue(throttle.isLockedOut(List.of(T0, T0.plusSeconds(1), T0.plusSeconds(2))));
    }

    @Test
    void retry_after_at_the_limit_is_the_oldest_failure_plus_the_window() {
        Instant now = T0.plusSeconds(30);
        List<Instant> failures = List.of(T0, T0.plusSeconds(10), T0.plusSeconds(20));
        // Unlocks when the oldest (T0) leaves the window: T0 + 15m - now(T0+30s).
        assertEquals(Duration.ofMinutes(15).minusSeconds(30), throttle.retryAfter(failures, now));
    }

    @Test
    void retry_after_over_the_limit_waits_for_the_nth_oldest_to_expire() {
        Instant now = T0.plusSeconds(50);
        // Five failures, limit three: the count drops below three only once the 3rd-oldest ages out.
        List<Instant> failures = List.of(T0, T0.plusSeconds(10), T0.plusSeconds(20),
                T0.plusSeconds(30), T0.plusSeconds(40));
        // index size-max = 5-3 = 2 -> T0+20s; +15m - now(T0+50s).
        assertEquals(Duration.ofMinutes(15).minusSeconds(30), throttle.retryAfter(failures, now));
    }

    @Test
    void retry_after_is_zero_when_not_locked_out() {
        assertEquals(Duration.ZERO, throttle.retryAfter(List.of(T0), T0.plusSeconds(1)));
    }

    @Test
    void retry_after_does_not_assume_the_input_is_ordered() {
        Instant now = T0.plusSeconds(30);
        List<Instant> shuffled = List.of(T0.plusSeconds(20), T0, T0.plusSeconds(10));
        assertEquals(Duration.ofMinutes(15).minusSeconds(30), throttle.retryAfter(shuffled, now));
    }

    @Test
    void rejects_a_nonsensical_policy() {
        assertThrows(InvalidLoginThrottleException.class, () -> new LoginThrottle(0, Duration.ofMinutes(1)));
        assertThrows(InvalidLoginThrottleException.class, () -> new LoginThrottle(3, Duration.ZERO));
        assertThrows(InvalidLoginThrottleException.class, () -> new LoginThrottle(3, Duration.ofMinutes(-1)));
    }
}
