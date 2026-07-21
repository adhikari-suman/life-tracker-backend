package com.lifetracker.application.session;

import com.lifetracker.domain.session.AccessToken;
import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.SessionId;

/**
 * The result of opening or rotating a Session: the Session's id, the raw refresh secret (the wire
 * format {@code sessionId.secret} is assembled at the web boundary), and the access token.
 */
public record IssuedSession(SessionId sessionId, RefreshTokenValue refreshToken, AccessToken accessToken) {
}
