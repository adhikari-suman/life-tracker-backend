package com.lifetracker.infrastructure.persistence.sharing;

import com.lifetracker.domain.sharing.ViewGrant;
import com.lifetracker.domain.sharing.ViewGrantId;
import com.lifetracker.domain.sharing.ViewGrantRepository;
import com.lifetracker.domain.user.UserId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** The {@link ViewGrantRepository} port, backed by Spring Data JPA. */
@Repository
class JpaViewGrantRepository implements ViewGrantRepository {

    private final ViewGrantJpaData data;

    JpaViewGrantRepository(ViewGrantJpaData data) {
        this.data = data;
    }

    @Override
    public void save(ViewGrant grant) {
        data.save(ViewGrantMapper.toEntity(grant));
    }

    @Override
    public Optional<ViewGrant> findById(ViewGrantId id) {
        return data.findById(id.value()).map(ViewGrantMapper::toDomain);
    }

    @Override
    public List<ViewGrant> findByOwnerId(UserId ownerId) {
        return data.findByOwnerUserId(ownerId.value()).stream().map(ViewGrantMapper::toDomain).toList();
    }

    @Override
    public Optional<ViewGrant> findByOwnerIdAndGranteeId(UserId ownerId, UserId granteeId) {
        return data.findByOwnerUserIdAndGranteeUserId(ownerId.value(), granteeId.value())
                .map(ViewGrantMapper::toDomain);
    }

    @Override
    public void deleteById(ViewGrantId id) {
        data.deleteById(id.value());
    }
}
