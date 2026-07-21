package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.sharing.ShareLink;
import com.lifetracker.domain.sharing.ShareLinkRepository;
import com.lifetracker.domain.sharing.ShareToken;
import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/** The {@link ShareLinkRepository} port, backed by Spring Data JPA. */
@Repository
class JpaShareLinkRepository implements ShareLinkRepository {

    private final ShareLinkJpaData data;

    JpaShareLinkRepository(ShareLinkJpaData data) {
        this.data = data;
    }

    @Override
    public void save(ShareLink shareLink) {
        data.save(ShareLinkMapper.toEntity(shareLink));
    }

    @Override
    public Optional<ShareLink> findByOwnerId(UserId ownerId) {
        return data.findByOwnerUserId(ownerId.value()).map(ShareLinkMapper::toDomain);
    }

    @Override
    public Optional<ShareLink> findByToken(ShareToken token) {
        return data.findByToken(token.value()).map(ShareLinkMapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByOwnerId(UserId ownerId) {
        data.deleteByOwnerUserId(ownerId.value());
    }
}
