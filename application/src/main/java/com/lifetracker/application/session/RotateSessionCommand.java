package com.lifetracker.application.session;

import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.SessionId;

/** Input to {@link RotateSession}: which Session, and the presented refresh secret to verify. */
public record RotateSessionCommand(SessionId sessionId, RefreshTokenValue presentedRefreshToken) {
}
