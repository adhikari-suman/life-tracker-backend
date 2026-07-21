package com.lifetracker.application.user;

/**
 * The input to {@link Authenticate}: the email and password as typed at login.
 */
public record AuthenticateCommand(String email, String password) {

    @Override
    public String toString() {
        // Never render the plaintext password, even into a log line.
        return "AuthenticateCommand[email=" + email + ", password=REDACTED]";
    }
}
