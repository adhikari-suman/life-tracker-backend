package com.lifetracker.domain.notification;

import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.user.Email;

/**
 * Delivers account emails to a User. A driven port: the transport — SMTP, a provider, or a stub that
 * only logs — lives in infrastructure. The use case hands over the recipient and the raw token; the
 * adapter turns it into a link. No SMTP in v1, so the only adapter today logs (ADR-0011).
 */
public interface EmailSender {

    /** Send an email-verification link carrying this token. */
    void sendEmailVerification(Email to, OneTimeTokenValue token);

    /** Send a password-reset link carrying this token. */
    void sendPasswordReset(Email to, OneTimeTokenValue token);
}
