package com.lifetracker.infrastructure.security;

import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.sharing.ShareTokens;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * The {@link ShareTokens} port: 32 bytes of {@link SecureRandom}, base64url-encoded. Unguessable,
 * which is the whole of a Share Link's security (ADR-0008) — it is stored as-is, not hashed.
 */
@Component
class SecureShareTokens implements ShareTokens {

    private static final int TOKEN_BYTES = 32;
    private static final Base64.Encoder BASE64URL = Base64.getUrlEncoder().withoutPadding();

    private final SecureRandom random = new SecureRandom();

    @Override
    public ShareToken generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return new ShareToken(BASE64URL.encodeToString(bytes));
    }
}
