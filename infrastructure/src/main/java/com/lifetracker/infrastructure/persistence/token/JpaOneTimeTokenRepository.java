package com.lifetracker.infrastructure.persistence.token;

import com.lifetracker.domain.token.OneTimeToken;
import com.lifetracker.domain.token.OneTimeTokenHash;
import com.lifetracker.domain.token.OneTimeTokenRepository;
import com.lifetracker.domain.token.TokenPurpose;
import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The {@link OneTimeTokenRepository} port, backed by Spring Data JPA. Speaks domain types. */
@Repository
class JpaOneTimeTokenRepository implements OneTimeTokenRepository {

    private final OneTimeTokenJpaData data;

    JpaOneTimeTokenRepository(OneTimeTokenJpaData data) {
        this.data = data;
    }

    @Override
    public void save(OneTimeToken token) {
        data.save(OneTimeTokenMapper.toEntity(token));
    }

    @Override
    public Optional<OneTimeToken> findByHash(OneTimeTokenHash hash) {
        return data.findByTokenHash(hash.value()).map(OneTimeTokenMapper::toDomain);
    }

    @Override
    public void delete(OneTimeToken token) {
        data.deleteById(token.id().value());
    }

    @Override
    @Transactional
    public void deleteByUserIdAndPurpose(UserId userId, TokenPurpose purpose) {
        data.deleteByUserIdAndPurpose(userId.value(), purpose.name());
    }
}
