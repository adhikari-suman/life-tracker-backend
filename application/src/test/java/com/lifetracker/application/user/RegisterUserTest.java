package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.WeakPasswordException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegisterUserTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final RegisterUser registerUser = new RegisterUser(users, new FakePasswordHasher());

    @Test
    void registers_a_new_user_and_stores_a_hashed_password() {
        UserId id = registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));

        User saved = users.findById(id).orElseThrow();
        assertEquals(new Email("sam@example.com"), saved.email());
        // The stored credential is the hash, never the plaintext.
        assertEquals("hashed:correct horse battery", saved.passwordHash().value());
        assertFalse(saved.isEmailVerified());
    }

    @Test
    void rejects_a_second_registration_of_the_same_email_case_insensitively() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));

        // SAM@ vs sam@ -- Email normalization makes uniqueness case-insensitive.
        assertThrows(EmailAlreadyRegisteredException.class,
                () -> registerUser.execute(new RegisterUserCommand("SAM@example.com", "another good password")));
    }

    @Test
    void rejects_a_weak_password_before_touching_the_store() {
        assertThrows(WeakPasswordException.class,
                () -> registerUser.execute(new RegisterUserCommand("sam@example.com", "short")));
        assertTrue(users.findByEmail(new Email("sam@example.com")).isEmpty());
    }

    @Test
    void rejects_a_malformed_email() {
        assertThrows(InvalidEmailException.class,
                () -> registerUser.execute(new RegisterUserCommand("not-an-email", "correct horse battery")));
    }
}
