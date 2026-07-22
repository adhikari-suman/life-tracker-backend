package com.lifetracker.infrastructure.notification;

import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.user.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The stub {@link EmailSender} for v1: there is no SMTP, so it logs what it would send (ADR-0011). A
 * real transport is a later adapter that replaces this one behind config — nothing else moves. The
 * raw token is logged deliberately, so a developer can complete the flow locally; a real adapter
 * would place it only inside the outbound email.
 */
@Component
class LoggingEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingEmailSender.class);

    @Override
    public void sendEmailVerification(Email to, OneTimeTokenValue token) {
        log.info("[stub email] verify {} with token {}", to.value(), token.value());
    }

    @Override
    public void sendPasswordReset(Email to, OneTimeTokenValue token) {
        log.info("[stub email] password reset for {} with token {}", to.value(), token.value());
    }
}
