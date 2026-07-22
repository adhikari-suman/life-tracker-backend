package com.lifetracker.application.user;

/** Input to {@link ResetPassword}: the reset token and the new password. Both are secrets. */
public record ResetPasswordCommand(String token, String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordCommand[token=REDACTED, newPassword=REDACTED]";
    }
}
