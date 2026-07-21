package com.lifetracker.infrastructure.security;

import com.lifetracker.domain.session.RefreshTokenHash;
import com.lifetracker.domain.session.RefreshTokenValue;
import com.lifetracker.domain.session.RefreshTokens;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The {@link RefreshTokens} port. A refresh secret is 32 bytes of {@link SecureRandom}, base64url-
 * encoded; because it is high-entropy, storing its plain SHA-256 (no salt) is enough, and a
 * presented secret is verified by re-hashing and comparing for equality.
 */
@Component
class Sha256RefreshTokens implements RefreshTokens {

    private static final int SECRET_BYTES = 32;
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    @Override
    public Issued issue() {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        RefreshTokenValue value = new RefreshTokenValue(BASE64URL.encodeToString(secret));
        return new Issued(value, hash(value));
    }

    @Override
    public RefreshTokenHash hash(RefreshTokenValue presented) {
        byte[] digest = sha256(presented.value().getBytes(StandardCharsets.UTF_8));
        return new RefreshTokenHash(BASE64URL.encodeToString(digest));
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
