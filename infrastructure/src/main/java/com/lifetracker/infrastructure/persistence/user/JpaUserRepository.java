package com.lifetracker.infrastructure.persistence.user;

import com.lifetracker.domain.user.Email;
import com.lifetracker.domain.user.User;
import com.lifetracker.domain.user.UserId;
import com.lifetracker.domain.user.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The {@link UserRepository} port, backed by Spring Data JPA. Speaks domain types; the entity and
 * the Spring Data interface stay behind it.
 */
@Repository
class JpaUserRepository implements UserRepository {

    private final UserJpaData data;

    JpaUserRepository(UserJpaData data) {
        this.data = data;
    }

    @Override
    public void save(User user) {
        data.save(UserMapper.toEntity(user));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return data.findById(id.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return data.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return data.existsByEmail(email.value());
    }
}
