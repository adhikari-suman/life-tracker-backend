package com.lifetracker.infrastructure.security;

import com.lifetracker.domain.user.PasswordHash;
import com.lifetracker.domain.user.PasswordHasher;
import com.lifetracker.domain.user.RawPassword;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * The {@link PasswordHasher} port, backed by Spring Security's Argon2 encoder (Argon2id, via
 * BouncyCastle). Parameters follow OWASP guidance for Argon2id: 19 MiB of memory, 2 iterations,
 * parallelism 1. The domain never sees any of this — it holds an opaque {@link PasswordHash}.
 */
@Component
class Argon2idPasswordHasher implements PasswordHasher {

    // saltLength=16, hashLength=32, parallelism=1, memory=19456 KiB (19 MiB), iterations=2
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);

    @Override
    public PasswordHash hash(RawPassword raw) {
        return new PasswordHash(encoder.encode(raw.value()));
    }

    @Override
    public boolean matches(RawPassword raw, PasswordHash hash) {
        return encoder.matches(raw.value(), hash.value());
    }
}
