package com.lifetracker.application.user;

import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticateTest {

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final RegisterUser registerUser = new RegisterUser(users, hasher);
    private final Authenticate authenticate = new Authenticate(users, hasher);

    @Test
    void authenticates_with_the_right_password() {
        UserId registered = registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        UserId authed = authenticate.execute(new AuthenticateCommand("sam@example.com", "correct horse battery"));
        assertEquals(registered, authed);
    }

    @Test
    void email_match_is_case_insensitive() {
        UserId registered = registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        UserId authed = authenticate.execute(new AuthenticateCommand("SAM@Example.com", "correct horse battery"));
        assertEquals(registered, authed);
    }

    @Test
    void rejects_a_wrong_password() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
    }

    @Test
    void rejects_an_unknown_email_indistinguishably() {
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("nobody@example.com", "correct horse battery")));
    }

    @Test
    void a_malformed_login_is_just_invalid_credentials_not_a_422() {
        // Authenticate never surfaces InvalidEmailException / WeakPasswordException: login is a
        // uniform 401, so a malformed attempt cannot be told from a wrong one.
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("not-an-email", "x")));
    }
}
