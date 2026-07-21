package com.lifetracker.application.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;

/**
 * Registers a new User: validate the email and password, reject a duplicate, hash the password,
 * and store the User. Returns the new {@link UserId}. Opening a Session and issuing tokens
 * (registration auto-logs-in) is a separate step the boundary composes on top of this.
 *
 * <p>Orchestrates; does not decide. Email and password validity live in the value objects; the
 * only rule here is uniqueness, which is a property of the store, not of one User.
 */
public final class RegisterUser {

    private final UserRepository users;
    private final PasswordHasher passwordHasher;

    public RegisterUser(UserRepository users, PasswordHasher passwordHasher) {
        this.users = users;
        this.passwordHasher = passwordHasher;
    }

    public UserId execute(RegisterUserCommand command) {
        Email email = new Email(command.email());              // InvalidEmailException  -> 422
        RawPassword raw = new RawPassword(command.password()); // WeakPasswordException  -> 422

        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email.value());
        }

        User user = User.register(UserId.generate(), email, passwordHasher.hash(raw));
        users.save(user);
        return user.id();
    }
}
