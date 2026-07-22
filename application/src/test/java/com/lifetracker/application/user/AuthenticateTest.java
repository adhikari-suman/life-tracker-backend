package com.lifetracker.application.user;

import com.lifetracker.domain.user.LoginThrottle;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthenticateTest {

    private static final Instant NOW = Instant.parse("2026-07-21T12:00:00Z");

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final InMemoryLoginAttempts loginAttempts = new InMemoryLoginAttempts();
    private final LoginThrottle throttle = new LoginThrottle(3, Duration.ofMinutes(15));
    private final RegisterUser registerUser = new RegisterUser(users, hasher);
    private final Authenticate authenticate =
            new Authenticate(users, hasher, loginAttempts, throttle, Clock.fixed(NOW, ZoneOffset.UTC));

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

    @Test
    void an_unknown_email_still_spends_a_verification_so_timing_does_not_leak_existence() {
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("ghost@example.com", "correct horse battery")));
        assertEquals(1, hasher.verifyInVainCount());
    }

    @Test
    void a_present_user_with_a_wrong_password_does_the_real_verify_not_the_dummy() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
        // The dummy verify is only for the no-user branch; a real user runs the real matches().
        assertEquals(0, hasher.verifyInVainCount());
    }

    @Test
    void locks_out_after_the_limit_of_failures() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        failThreeTimes("sam@example.com");
        assertThrows(TooManyAttemptsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
    }

    @Test
    void the_lockout_carries_how_long_to_wait() {
        failThreeTimes("sam@example.com");
        TooManyAttemptsException locked = assertThrows(TooManyAttemptsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
        // Every failure shares the fixed clock, so the window unlocks a full window from now.
        assertEquals(Duration.ofMinutes(15), locked.retryAfter());
    }

    @Test
    void an_unknown_email_is_counted_and_locks_out_too() {
        failThreeTimes("ghost@example.com"); // never registered
        assertThrows(TooManyAttemptsException.class,
                () -> authenticate.execute(new AuthenticateCommand("ghost@example.com", "wrong password here")));
    }

    @Test
    void a_successful_login_clears_the_counter() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
        assertThrows(InvalidCredentialsException.class,
                () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
        authenticate.execute(new AuthenticateCommand("sam@example.com", "correct horse battery"));
        assertEquals(0, loginAttempts.totalFor("sam@example.com"));
    }

    @Test
    void a_throttled_attempt_is_not_itself_recorded() {
        registerUser.execute(new RegisterUserCommand("sam@example.com", "correct horse battery"));
        failThreeTimes("sam@example.com");
        for (int i = 0; i < 5; i++) { // keep knocking while locked out
            assertThrows(TooManyAttemptsException.class,
                    () -> authenticate.execute(new AuthenticateCommand("sam@example.com", "wrong password here")));
        }
        // The recorded count never grew past the limit, so the lockout still drains on schedule.
        assertEquals(3, loginAttempts.totalFor("sam@example.com"));
    }

    private void failThreeTimes(String email) {
        for (int i = 0; i < 3; i++) {
            assertThrows(InvalidCredentialsException.class,
                    () -> authenticate.execute(new AuthenticateCommand(email, "wrong password here")));
        }
    }
}
