package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.InvalidEmailException;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;
import com.lifetracker.domain.user.WeakPasswordException;

/**
 * Verifies an email + password and returns the authenticated {@link UserId}. Issuing the access
 * token and opening the Session are separate, later steps — this use case decides only "is this
 * the right password for this user?".
 *
 * <p>Every failure raises the same {@link InvalidCredentialsException}: a wrong password, an
 * unknown email, and a malformed input are indistinguishable, so login leaks nothing about which
 * emails exist.
 */
public final class Authenticate {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public Authenticate(UserRepository users, PasswordHasher passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    public UserId execute(AuthenticateCommand command) {
        Email email;
        RawPassword raw;
        try {
            email = new Email(command.email());
            raw = new RawPassword(command.password());
        } catch (InvalidEmailException | WeakPasswordException malformed) {
            // A malformed email, or a password too short to ever be a stored one, is simply a
            // failed login -- never a 422 here, and never distinguishable from a wrong password.
            throw new InvalidCredentialsException();
        }

        // NOTE (deferred hardening): when the user is absent we skip the hash and return fast,
        // which is a timing oracle for "does this email exist?". Equalize later with a dummy
        // verify in the hasher. Tracked alongside brute-force protection (ADR-0007).
        User user = users.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(raw, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return user.id();
    }
}
