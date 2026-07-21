package com.lifetracker.domain.user;

/**
 * Hashes and verifies passwords. A port: the algorithm (Argon2id) and its parameters are an
 * infrastructure concern, kept out of the domain so no crypto library is even importable here.
 * The domain knows only that a {@link RawPassword} can be turned into a {@link PasswordHash} and
 * checked against one.
 */
public interface PasswordHasher {

    PasswordHash hash(RawPassword raw);

    boolean matches(RawPassword raw, PasswordHash hash);
}
