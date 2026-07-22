package com.lifetracker.infrastructure.security;

import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenValue;
import com.lifetracker.domain.token.OneTimeTokens;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * The {@link OneTimeTokens} port. A token secret is 32 bytes of {@link SecureRandom}, base64url-
 * encoded; high-entropy, so its plain SHA-256 (no salt) is enough, and a presented secret is looked
 * up by re-hashing. The same construction as the refresh-token adapter.
 */
@Component
class Sha256OneTimeTokens implements OneTimeTokens {

    private static final int SECRET_BYTES = 32;
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    @Override
    public Issued issue() {
        byte[] secret = new byte[SECRET_BYTES];
        random.nextBytes(secret);
        OneTimeTokenValue value = new OneTimeTokenValue(BASE64URL.encodeToString(secret));
        return new Issued(value, hash(value));
    }

    @Override
    public OneTimeTokenHash hash(OneTimeTokenValue presented) {
        byte[] digest = sha256(presented.value().getBytes(StandardCharsets.UTF_8));
        return new OneTimeTokenHash(BASE64URL.encodeToString(digest));
    }

    private static byte[] sha256(byte[] input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
