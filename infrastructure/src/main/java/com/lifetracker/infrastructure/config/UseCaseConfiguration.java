package com.lifetracker.infrastructure.config;

import com.lifetracker.application.account.OpenAccount;
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
import com.lifetracker.application.user.RequestPasswordReset;
import com.lifetracker.application.user.ResetPassword;
import com.lifetracker.application.user.SendEmailVerification;
import com.lifetracker.application.user.VerifyEmail;
import com.lifetracker.application.transaction.RecordTransaction;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.notification.EmailSender;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.RefreshTokens;
import com.lifetracker.domain.session.SessionRepository;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareTokens;
import com.lifetracker.domain.sharing.ViewGrantRepository;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.OneTimeTokens;
import com.lifetracker.domain.transaction.TransactionRepository;
import com.lifetracker.domain.user.LoginAttempts;
import com.lifetracker.domain.user.LoginThrottle;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.UserRepository;
import com.lifetracker.infrastructure.security.AccountTokenProperties;
import com.lifetracker.infrastructure.security.LoginThrottleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Wires the plain-Java use cases as Spring beans. The use cases carry no Spring annotations — the
 * application module has no Spring on its classpath — so infrastructure constructs them here from
 * the port beans (the JPA repositories, the Argon2 hasher, the RS256 / SHA-256 token adapters, the
 * share-token generator, the email sender).
 */
@Configuration
@EnableConfigurationProperties({LoginThrottleProperties.class, AccountTokenProperties.class})
class UseCaseConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    OpenAccount openAccount(AccountRepository accounts) {
        return new OpenAccount(accounts);
    }

    @Bean
    RecordTransaction recordTransaction(AccountRepository accounts, TransactionRepository transactions) {
        return new RecordTransaction(accounts, transactions);
    }

    @Bean
    RegisterUser registerUser(UserRepository users, PasswordHasher passwordHasher) {
        return new RegisterUser(users, passwordHasher);
    }

    @Bean
    LoginThrottle loginThrottle(LoginThrottleProperties properties) {
        return new LoginThrottle(properties.maxAttempts(), properties.window());
    }

    @Bean
    Authenticate authenticate(UserRepository users, PasswordHasher passwordHasher,
                              LoginAttempts loginAttempts, LoginThrottle loginThrottle, Clock clock) {
        return new Authenticate(users, passwordHasher, loginAttempts, loginThrottle, clock);
    }

    @Bean
    SendEmailVerification sendEmailVerification(UserRepository users, OneTimeTokens tokens,
                                                OneTimeTokenRepository tokenStore, EmailSender emailSender,
                                                AccountTokenProperties properties, Clock clock) {
        return new SendEmailVerification(users, tokens, tokenStore, emailSender, properties.verificationTtl(), clock);
    }

    @Bean
    VerifyEmail verifyEmail(OneTimeTokens tokens, OneTimeTokenRepository tokenStore, UserRepository users, Clock clock) {
        return new VerifyEmail(tokens, tokenStore, users, clock);
    }

    @Bean
    RequestPasswordReset requestPasswordReset(UserRepository users, OneTimeTokens tokens,
                                              OneTimeTokenRepository tokenStore, EmailSender emailSender,
                                              AccountTokenProperties properties, Clock clock) {
        return new RequestPasswordReset(users, tokens, tokenStore, emailSender, properties.passwordResetTtl(), clock);
    }

    @Bean
    ResetPassword resetPassword(OneTimeTokens tokens, OneTimeTokenRepository tokenStore, UserRepository users,
                                PasswordHasher passwordHasher, SessionRepository sessions, Clock clock) {
        return new ResetPassword(tokens, tokenStore, users, passwordHasher, sessions, clock);
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
    CreateShareLink createShareLink(ShareLinkRepository shareLinks, ShareTokens shareTokens,
                                    UserRepository users, Clock clock) {
        return new CreateShareLink(shareLinks, shareTokens, users, clock);
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
