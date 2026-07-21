package com.lifetracker.application.session;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionRepository;

/**
 * Ends one Session — logout, or "sign out this device". Revokes only if the Session belongs to the
 * requesting User; an unknown or someone-else's Session is reported as
 * {@link SessionNotFoundException}, never revealing another User's Sessions. Idempotent: revoking an
 * already-revoked Session is fine (the Session is soft-revoked, not deleted).
 */
public final class RevokeSession {

    private final SessionRepository sessions;

    public RevokeSession(SessionRepository sessions) {
        this.sessions = sessions;
    }

    public void execute(RevokeSessionCommand command) {
        Session session = sessions.findById(command.sessionId())
                .filter(s -> s.userId().equals(command.userId()))
                .orElseThrow(SessionNotFoundException::new);
        session.revoke();
        sessions.save(session);
    }
}
