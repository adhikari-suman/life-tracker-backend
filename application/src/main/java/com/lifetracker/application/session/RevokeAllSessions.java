package com.lifetracker.application.session;

import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionRepository;

/**
 * Ends every Session a User has — "sign out everywhere". Loads each Session and revokes it, so the
 * revocation rule stays in the aggregate rather than becoming a bulk UPDATE.
 */
public final class RevokeAllSessions {

    private final SessionRepository sessions;

    public RevokeAllSessions(SessionRepository sessions) {
        this.sessions = sessions;
    }

    public void execute(RevokeAllSessionsCommand command) {
        for (Session session : sessions.findByUserId(command.userId())) {
            session.revoke();
            sessions.save(session);
        }
    }
}
