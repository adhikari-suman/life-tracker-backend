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

    /**
     * Spend the cost of a verification whose result is thrown away. The no-user branch of login
     * calls this so an unknown email takes the same wall-clock as a real one, denying an attacker
     * the timing signal for "does this email exist?" (ADR-0007). It does the same work as
     * {@link #matches}; it simply has nothing real to check against.
     */
    void verifyInVain(RawPassword raw);
}
