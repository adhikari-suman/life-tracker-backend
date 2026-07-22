package com.lifetracker.infrastructure.web;

import com.lifetracker.application.session.IssuedSession;
import com.lifetracker.application.session.OpenSession;
import com.lifetracker.application.session.OpenSessionCommand;
import com.lifetracker.application.session.RevokeSession;
import com.lifetracker.application.session.RevokeSessionCommand;
import com.lifetracker.application.session.RotateSession;
import com.lifetracker.application.session.RotateSessionCommand;
import com.lifetracker.application.user.Authenticate;
import com.lifetracker.application.user.AuthenticateCommand;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.application.user.RegisterUserCommand;
import com.lifetracker.application.user.SendEmailVerification;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.infrastructure.web.dto.LoginRequest;
import com.lifetracker.infrastructure.web.dto.RefreshRequest;
import com.lifetracker.infrastructure.web.dto.RegisterRequest;
import com.lifetracker.infrastructure.web.dto.TokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The authentication endpoints. Thin: parse, call the use cases, assemble the response. Register,
 * login and refresh are public and each open a Session, so all three return the same
 * {@link TokenResponse}. Logout requires a token and ends the current Session. Errors map to
 * RFC 7807 in {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/auth")
class AuthController {

    private static final int MAX_DEVICE_LABEL = 200;

    private final RegisterUser registerUser;
    private final Authenticate authenticate;
    private final SendEmailVerification sendEmailVerification;
    private final OpenSession openSession;
    private final RotateSession rotateSession;
    private final RevokeSession revokeSession;

    AuthController(RegisterUser registerUser, Authenticate authenticate,
                   SendEmailVerification sendEmailVerification, OpenSession openSession,
                   RotateSession rotateSession, RevokeSession revokeSession) {
        this.registerUser = registerUser;
        this.authenticate = authenticate;
        this.sendEmailVerification = sendEmailVerification;
        this.openSession = openSession;
        this.rotateSession = rotateSession;
        this.revokeSession = revokeSession;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    TokenResponse register(@Valid @RequestBody RegisterRequest request,
                           @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        UserId userId = registerUser.execute(new RegisterUserCommand(request.email(), request.password()));
        sendEmailVerification.execute(userId);
        return tokenResponse(openSession.execute(new OpenSessionCommand(userId, deviceLabel(null, userAgent))));
    }

    @PostMapping("/login")
    TokenResponse login(@Valid @RequestBody LoginRequest request,
                        @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        UserId userId = authenticate.execute(new AuthenticateCommand(request.email(), request.password()));
        return tokenResponse(openSession.execute(
                new OpenSessionCommand(userId, deviceLabel(request.deviceLabel(), userAgent))));
    }

    @PostMapping("/refresh")
    TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
        WireRefreshToken.Parsed parsed = WireRefreshToken.decode(request.refreshToken());
        return tokenResponse(rotateSession.execute(
                new RotateSessionCommand(parsed.sessionId(), parsed.secret())));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal Jwt jwt) {
        revokeSession.execute(new RevokeSessionCommand(AuthPrincipal.currentSession(jwt), AuthPrincipal.userId(jwt)));
    }

    private static TokenResponse tokenResponse(IssuedSession issued) {
        return TokenResponse.bearer(
                issued.accessToken().value(),
                issued.accessToken().expiresInSeconds(),
                WireRefreshToken.encode(issued.sessionId(), issued.refreshToken()));
    }

    private static String deviceLabel(String fromBody, String userAgent) {
        String label = (fromBody != null && !fromBody.isBlank()) ? fromBody
                : (userAgent != null && !userAgent.isBlank()) ? userAgent
                : "unknown";
        return label.length() > MAX_DEVICE_LABEL ? label.substring(0, MAX_DEVICE_LABEL) : label;
    }
}
