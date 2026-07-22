package com.lifetracker.application.user;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.UserId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** In-memory {@link OneTimeTokenRepository} fake — a list of tokens, looked up by hash. */
final class InMemoryOneTimeTokenRepository implements OneTimeTokenRepository {

    private final List<OneTimeToken> tokens = new ArrayList<>();

    @Override
    public void save(OneTimeToken token) {
        tokens.add(token);
    }

    @Override
    public Optional<OneTimeToken> findByHash(OneTimeTokenHash hash) {
        return tokens.stream().filter(t -> t.tokenHash().equals(hash)).findFirst();
    }

    @Override
    public void delete(OneTimeToken token) {
        tokens.removeIf(t -> t.id().equals(token.id()));
    }

    @Override
    public void deleteByUserIdAndPurpose(UserId userId, TokenPurpose purpose) {
        tokens.removeIf(t -> t.userId().equals(userId) && t.isFor(purpose));
    }

    int size() {
        return tokens.size();
    }

    long countFor(UserId userId, TokenPurpose purpose) {
        return tokens.stream().filter(t -> t.userId().equals(userId) && t.isFor(purpose)).count();
    }
}
