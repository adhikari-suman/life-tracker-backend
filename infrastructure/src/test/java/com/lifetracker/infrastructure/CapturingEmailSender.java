package com.lifetracker.infrastructure;

import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.user.Email;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.List;

/**
 * Test {@link EmailSender} that captures what would be sent, so an integration test can pull the
 * token out and complete the flow. {@code @Primary} so it replaces the production
 * {@code LoggingEmailSender}; imported by {@link AbstractIntegrationTest}, so every integration test
 * has it.
 */
@Primary
class CapturingEmailSender implements EmailSender {

    enum Kind { VERIFICATION, PASSWORD_RESET }

    record Sent(Kind kind, String email, String token) {
    }

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public void sendEmailVerification(Email to, OneTimeTokenValue token) {
        sent.add(new Sent(Kind.VERIFICATION, to.value(), token.value()));
    }

    @Override
    public void sendPasswordReset(Email to, OneTimeTokenValue token) {
        sent.add(new Sent(Kind.PASSWORD_RESET, to.value(), token.value()));
    }

    /** The most recent token of a kind sent to this email. Fails the test if none was captured. */
    String latestToken(Kind kind, String email) {
        return sent.stream()
                .filter(s -> s.kind() == kind && s.email().equals(email))
                .reduce((first, second) -> second)
                .map(Sent::token)
                .orElseThrow(() -> new AssertionError("no " + kind + " email captured for " + email));
    }

    boolean hasSentTo(String email) {
        return sent.stream().anyMatch(s -> s.email().equals(email));
    }
}
