package com.lifetracker.application.user;

import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.user.Email;

import java.util.ArrayList;
import java.util.List;

/** Records what would be sent, so a use-case test can assert on it. */
final class RecordingEmailSender implements EmailSender {

    record Sent(String kind, String email, String token) {
    }

    final List<Sent> sent = new ArrayList<>();

    @Override
    public void sendEmailVerification(Email to, OneTimeTokenValue token) {
        sent.add(new Sent("VERIFY", to.value(), token.value()));
    }

    @Override
    public void sendPasswordReset(Email to, OneTimeTokenValue token) {
        sent.add(new Sent("RESET", to.value(), token.value()));
    }

    Sent last() {
        return sent.get(sent.size() - 1);
    }
}
