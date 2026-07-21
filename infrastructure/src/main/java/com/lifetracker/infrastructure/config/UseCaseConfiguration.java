package com.lifetracker.infrastructure.config;

import com.lifetracker.application.session.OpenSession;
import com.lifetracker.application.session.RevokeAllSessions;
import com.lifetracker.application.session.RevokeSession;
import com.lifetracker.application.session.RotateSession;
import com.lifetracker.application.sharing.CreateShareLink;
import com.lifetracker.application.sharing.GrantView;
import com.lifetracker.application.sharing.RevokeShareLink;
import com.lifetracker.application.sharing.RevokeView;
import com.lifetracker.application.user.Authenticate;
import com.lifetracker.application.user.RegisterUser;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.RefreshTokens;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareTokens;
import com.lifetracker.domain.sharing.ViewGrantRepository;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the plain-Java use cases as Spring beans. The use cases carry no Spring annotations — the
 * application module has no Spring on its classpath — so infrastructure constructs them here from
 * the port beans (the JPA repositories, the Argon2 hasher, the RS256 / SHA-256 token adapters, the
 * share-token generator).
 */
@Configuration
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    RegisterUser registerUser(UserRepository users, PasswordHasher passwordHasher) {
        return new RegisterUser(users, passwordHasher);
    }

    @Bean
    Authenticate authenticate(UserRepository users, PasswordHasher passwordHasher) {
        return new Authenticate(users, passwordHasher);
    }

    @Bean
    OpenSession openSession(SessionRepository sessions, RefreshTokens refreshTokens,
                            AccessTokens accessTokens, Clock clock) {
        return new OpenSession(sessions, refreshTokens, accessTokens, clock);
    }

    @Bean
    RotateSession rotateSession(SessionRepository sessions, RefreshTokens refreshTokens,
                                AccessTokens accessTokens, Clock clock) {
        return new RotateSession(sessions, refreshTokens, accessTokens, clock);
    }

    @Bean
    RevokeSession revokeSession(SessionRepository sessions) {
        return new RevokeSession(sessions);
    }

    @Bean
    RevokeAllSessions revokeAllSessions(SessionRepository sessions) {
        return new RevokeAllSessions(sessions);
    }

    @Bean
    CreateShareLink createShareLink(ShareLinkRepository shareLinks, ShareTokens shareTokens, Clock clock) {
        return new CreateShareLink(shareLinks, shareTokens, clock);
    }

    @Bean
    RevokeShareLink revokeShareLink(ShareLinkRepository shareLinks) {
        return new RevokeShareLink(shareLinks);
    }

    @Bean
    GrantView grantView(ViewGrantRepository grants, UserRepository users, Clock clock) {
        return new GrantView(grants, users, clock);
    }

    @Bean
    RevokeView revokeView(ViewGrantRepository grants) {
        return new RevokeView(grants);
    }
}
