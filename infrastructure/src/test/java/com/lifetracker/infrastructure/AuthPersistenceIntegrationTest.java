package com.lifetracker.infrastructure;

import com.lifetracker.application.user.Authenticate;
import com.lifetracker.application.user.AuthenticateCommand;
import com.lifetracker.application.user.EmailAlreadyRegisteredException;
import com.lifetracker.application.user.InvalidCredentialsException;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.application.user.RegisterUserCommand;
import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end persistence against real Postgres (Testcontainers), a Liquibase-applied schema, and
 * Hibernate {@code validate}. The context booting at all IS the drift check — it proves
 * {@code UserEntity} matches the 001-create-users migration. The assertions then prove
 * register -> persist -> authenticate works and that a real Argon2id hash is stored, never the
 * plaintext. Each test uses a distinct email, so methods do not interfere.
 */
class AuthPersistenceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    Authenticate authenticate;

    @Autowired
    UserRepository users;

    @Test
    void registers_persists_and_authenticates() {
        UserId id = registerUser.execute(new RegisterUserCommand("roundtrip@example.com", "correct horse battery"));

        User stored = users.findByEmail(new Email("roundtrip@example.com")).orElseThrow();
        assertEquals(id, stored.id());

        UserId authed = authenticate.execute(new AuthenticateCommand("roundtrip@example.com", "correct horse battery"));
        assertEquals(id, authed);
    }

    @Test
    void stores_a_real_argon2id_hash_never_the_plaintext() {
        registerUser.execute(new RegisterUserCommand("hash@example.com", "correct horse battery"));

        User stored = users.findByEmail(new Email("hash@example.com")).orElseThrow();
        assertTrue(stored.passwordHash().value().startsWith("$argon2id$"),
                "expected an argon2id-encoded hash, got: " + stored.passwordHash().value());
        assertNotEquals("correct horse battery", stored.passwordHash().value());
    }

    @Test
    void rejects_a_wrong_password() {
        registerUser.execute(new RegisterUserCommand("wrongpw@example.com", "correct horse battery"));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("wrongpw@example.com", "not the password")));
    }

    @Test
    void rejects_a_duplicate_registration() {
        registerUser.execute(new RegisterUserCommand("dup@example.com", "correct horse battery"));
        assertThrows(EmailAlreadyRegisteredException.class,
                () -> registerUser.execute(new RegisterUserCommand("dup@example.com", "another good password")));
    }
}
