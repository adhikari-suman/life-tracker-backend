package com.lifetracker.infrastructure.security;

import com.lifetracker.domain.session.AccessToken;
import com.lifetracker.domain.session.AccessTokens;
import com.lifetracker.domain.session.SessionId;
import com.lifetracker.domain.user.UserId;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * The {@link AccessTokens} port, backed by an RS256 {@link JwtEncoder}. Mints a JWT naming the User
 * (subject) and the Session (the {@code sid} claim), valid for the configured TTL. The {@code kid}
 * header is added by the encoder from the signing JWK.
 */
@Component
class Rs256AccessTokens implements AccessTokens {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final Duration ttl;

    Rs256AccessTokens(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = properties.issuer();
        this.ttl = properties.accessTokenTtl();
    }

    @Override
    public AccessToken issueFor(UserId userId, SessionId sessionId, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(userId.value().toString())
                .claim("sid", sessionId.value().toString())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new AccessToken(token, ttl.getSeconds());
    }
}
