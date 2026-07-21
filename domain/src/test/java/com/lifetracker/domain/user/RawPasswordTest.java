package com.lifetracker.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RawPasswordTest {

    @Test
    void accepts_a_password_of_sufficient_length() {
        assertDoesNotThrow(() -> new RawPassword("correct horse battery"));
    }

    @Test
    void rejects_a_password_below_the_minimum() {
        assertThrows(WeakPasswordException.class, () -> new RawPassword("short"));
    }

    @Test
    void toString_does_not_leak_the_password() {
        assertFalse(new RawPassword("correct horse battery").toString().contains("horse"));
    }
}
