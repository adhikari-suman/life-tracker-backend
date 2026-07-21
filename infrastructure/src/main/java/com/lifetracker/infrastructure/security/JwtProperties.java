package com.lifetracker.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

import java.time.Duration;

/**
 * RS256 signing configuration, bound from {@code app.jwt.*}. The private key is the secret; in prod
 * it is supplied from the environment. The dev/test keypair lives under test resources and is a
 * throwaway (option B — keys loaded from config, see the token slice).
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(Resource privateKey, Resource publicKey, String issuer, Duration accessTokenTtl) {
}
