package com.lifetracker.application.user;

/**
 * The input to {@link RegisterUser}: raw strings from the boundary. The use case turns these into
 * domain value objects, which is where email and password validity get decided.
 */
public record RegisterUserCommand(String email, String password) {

    @Override
    public String toString() {
        // Never render the plaintext password, even into a log line.
        return "RegisterUserCommand[email=" + email + ", password=REDACTED]";
    }
}
