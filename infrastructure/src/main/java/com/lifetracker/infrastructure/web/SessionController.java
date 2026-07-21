package com.lifetracker.infrastructure.web;

import com.lifetracker.application.session.RevokeAllSessions;
import com.lifetracker.application.session.RevokeAllSessionsCommand;
import com.lifetracker.application.session.RevokeSession;
import com.lifetracker.application.session.RevokeSessionCommand;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.infrastructure.persistence.session.SessionQueryService;
import com.lifetracker.infrastructure.web.dto.SessionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * The authenticated User's Sessions — the "active devices" screen and its controls. Listing is a
 * read (query service); revoking one, or all ("sign out everywhere"), are writes (use cases). A
 * Session that is not the caller's is reported as not found, never revealed.
 */
@RestController
@RequestMapping("/auth/sessions")
class SessionController {

    private final SessionQueryService sessions;
    private final RevokeSession revokeSession;
    private final RevokeAllSessions revokeAllSessions;

    SessionController(SessionQueryService sessions, RevokeSession revokeSession, RevokeAllSessions revokeAllSessions) {
        this.sessions = sessions;
        this.revokeSession = revokeSession;
        this.revokeAllSessions = revokeAllSessions;
    }

    @GetMapping
    List<SessionResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UserId userId = AuthPrincipal.userId(jwt);
        UUID current = AuthPrincipal.currentSession(jwt).value();
        return sessions.findActiveByUser(userId).stream()
                .map(view -> new SessionResponse(
                        view.id(), view.deviceLabel(), view.createdAt(), view.lastUsedAt(), view.id().equals(current)))
                .toList();
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sessionId) {
        revokeSession.execute(new RevokeSessionCommand(SessionId.of(sessionId), AuthPrincipal.userId(jwt)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revokeAll(@AuthenticationPrincipal Jwt jwt) {
        revokeAllSessions.execute(new RevokeAllSessionsCommand(AuthPrincipal.userId(jwt)));
    }
}
