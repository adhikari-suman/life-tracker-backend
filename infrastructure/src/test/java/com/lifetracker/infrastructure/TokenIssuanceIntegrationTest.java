package com.lifetracker.infrastructure;

import com.lifetracker.application.session.InvalidRefreshTokenException;
import com.lifetracker.application.session.IssuedSession;
import com.lifetracker.application.session.OpenSession;
import com.lifetracker.application.session.OpenSessionCommand;
import com.lifetracker.application.session.RotateSession;
import com.lifetracker.application.session.RotateSessionCommand;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.application.user.RegisterUserCommand;
import com.lifetracker.domain.session.RefreshTokens;
import com.lifetracker.domain.session.Session;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.user.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end token issuance with REAL crypto: the RS256 encoder signs with the config-loaded private
 * key and the decoder verifies with the public key; refresh secrets are SHA-256 hashed. Proves the
 * whole session token flow against Postgres.
 */
class TokenIssuanceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RegisterUser registerUser;

    @Autowired
    OpenSession openSession;

    @Autowired
    RotateSession rotateSession;

    @Autowired
    SessionRepository sessions;

    @Autowired
    RefreshTokens refreshTokens;

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void issues_a_real_rs256_access_token_and_a_matching_refresh_hash() {
        UserId userId = registerUser.execute(new RegisterUserCommand("token@example.com", "correct horse battery"));

        IssuedSession issued = openSession.execute(new OpenSessionCommand(userId, "Chrome on Mac"));

        // The access token is a real RS256 JWT: it verifies against the public key and carries the claims.
        Jwt jwt = jwtDecoder.decode(issued.accessToken().value());
        assertTrue(jwt.getHeaders().get("alg").toString().contains("RS256"));
        assertNotNull(jwt.getHeaders().get("kid"));
        assertEquals(userId.value().toString(), jwt.getSubject());
        assertEquals(issued.sessionId().value().toString(), jwt.getClaimAsString("sid"));
        assertEquals("life-tracker-test", jwt.getClaimAsString("iss"));

        // The stored refresh hash is the SHA-256 of the secret we handed back — never the secret itself.
        Session stored = sessions.findById(issued.sessionId()).orElseThrow();
        assertEquals(refreshTokens.hash(issued.refreshToken()), stored.refreshTokenHash());
        assertNotEquals(issued.refreshToken().value(), stored.refreshTokenHash().value());
    }

    @Test
    void rotating_then_replaying_the_old_token_revokes_the_session() {
        UserId userId = registerUser.execute(new RegisterUserCommand("token-rotate@example.com", "correct horse battery"));
        IssuedSession opened = openSession.execute(new OpenSessionCommand(userId, "device"));

        IssuedSession rotated =
                rotateSession.execute(new RotateSessionCommand(opened.sessionId(), opened.refreshToken()));
        assertNotEquals(opened.refreshToken(), rotated.refreshToken());

        assertThrows(InvalidRefreshTokenException.class,
                () -> rotateSession.execute(new RotateSessionCommand(opened.sessionId(), opened.refreshToken())));
        assertTrue(sessions.findById(opened.sessionId()).orElseThrow().isRevoked());
    }
}
