package com.lifetracker.application.session;

import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;

/** Input to {@link RevokeSession}: the Session to end, and the User it must belong to. */
public record RevokeSessionCommand(SessionId sessionId, UserId userId) {
}
