package com.lifetracker.infrastructure.web;

import com.lifetracker.application.session.InvalidRefreshTokenException;
import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.SessionId;

import java.util.UUID;

/**
 * The wire form of a refresh token: {@code <sessionId>.<secret>}. The client never sees the raw
 * secret alone — it needs the session id to look the Session up. Encoded on responses, split back
 * on {@code /auth/refresh}. A malformed value is an invalid refresh token (401), indistinguishable
 * from an expired or replayed one.
 */
final class WireRefreshToken {

    private WireRefreshToken() {
    }

    static String encode(SessionId sessionId, RefreshTokenValue secret) {
        return sessionId.value() + "." + secret.value();
    }

    record Parsed(SessionId sessionId, RefreshTokenValue secret) {
    }

    static Parsed decode(String wire) {
        int dot = wire.indexOf('.');
        if (dot <= 0 || dot == wire.length() - 1) {
            throw new InvalidRefreshTokenException();
        }
        try {
            SessionId sessionId = SessionId.of(UUID.fromString(wire.substring(0, dot)));
            RefreshTokenValue secret = new RefreshTokenValue(wire.substring(dot + 1));
            return new Parsed(sessionId, secret);
        } catch (IllegalArgumentException | IllegalStateException malformed) {
            throw new InvalidRefreshTokenException();
        }
    }
}
