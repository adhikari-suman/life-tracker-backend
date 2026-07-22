package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.LoginAttempts;
import com.lifetracker.domain.user.LoginThrottle;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;
import com.lifetracker.domain.user.WeakPasswordException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Verifies an email + password and returns the authenticated {@link UserId}. Issuing the access
 * token and opening the Session are separate, later steps — this use case decides only "is this
 * the right password for this user?".
 *
 * <p>Every credential failure raises the same {@link InvalidCredentialsException}: a wrong password,
 * an unknown email, and a malformed input are indistinguishable, so login leaks nothing about which
 * emails exist -- including through timing, since the unknown-email path spends a dummy verification
 * rather than returning early.
 *
 * <p>Brute force is bounded per email by a {@link LoginThrottle} (ADR-0010): once too many failures
 * land within the trailing window, an attempt raises {@link TooManyAttemptsException} <em>before</em>
 * the password is looked at. Failures are counted against the submitted email whether or not a User
 * owns it, so a lockout — like every other failure here — reveals nothing about existence. A
 * successful login clears the count; an attempt already rejected by the throttle is not itself
 * recorded, so a lockout lasts one window past the last real failure, not for as long as an attacker
 * keeps knocking.
 */
public final class Authenticate {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final LoginAttempts loginAttempts;
    private final LoginThrottle throttle;
    private final Clock clock;

    public Authenticate(UserRepository users, PasswordHasher passwordHasher, LoginAttempts loginAttempts,
                        LoginThrottle throttle, Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.loginAttempts = loginAttempts;
        this.throttle = throttle;
        this.clock = clock;
    }

    public UserId execute(AuthenticateCommand command) {
        Email email;
        RawPassword raw;
        try {
            email = new Email(command.email());
            raw = new RawPassword(command.password());
        } catch (InvalidEmailException | WeakPasswordException malformed) {
            // A malformed email, or a password too short to ever be a stored one, is simply a
            // failed login -- never a 422 here, and never distinguishable from a wrong password. It
            // can never match a stored credential either, so it is not worth a throttle slot.
            throw new InvalidCredentialsException();
        }

        Instant now = clock.instant();
        List<Instant> recentFailures = loginAttempts.failuresSince(email, now.minus(throttle.window()));
        if (throttle.isLockedOut(recentFailures)) {
            throw new TooManyAttemptsException(throttle.retryAfter(recentFailures, now));
        }

        Optional<User> user = users.findByEmail(email);
        boolean authenticated;
        if (user.isPresent()) {
            authenticated = passwordHasher.matches(raw, user.get().passwordHash());
        } else {
            // No such user -- but spend a verification anyway, so an unknown email costs the same
            // wall-clock as a real one. Returning early here would let response time answer "does
            // this email exist?", the very leak the uniform 401 exists to prevent (ADR-0007).
            passwordHasher.verifyInVain(raw);
            authenticated = false;
        }
        if (!authenticated) {
            loginAttempts.recordFailure(email, now);
            throw new InvalidCredentialsException();
        }

        loginAttempts.clearFailures(email);
        return user.get().id();
    }
}
