package com.lifetracker.domain.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain JUnit. No Spring, no database, no mocks — a value object is testable with {@code new}.
 */
class EmailTest {

    @Test
    void normalizes_case_and_surrounding_whitespace() {
        // Same address, differently typed — must be one value so uniqueness is case-insensitive.
        assertEquals(new Email("sam@example.com"), new Email("  Sam@Example.COM  "));
    }

    @Test
    void exposes_the_normalized_value() {
        assertEquals("sam@example.com", new Email("Sam@Example.com").value());
    }

    @Test
    void rejects_a_value_with_no_at_sign() {
        assertThrows(InvalidEmailException.class, () -> new Email("samexample.com"));
    }

    @Test
    void rejects_a_domain_with_no_dot() {
        assertThrows(InvalidEmailException.class, () -> new Email("sam@example"));
    }

    @Test
    void rejects_blank() {
        assertThrows(InvalidEmailException.class, () -> new Email("   "));
    }
}
