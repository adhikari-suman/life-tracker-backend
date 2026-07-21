package com.lifetracker.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data access to the {@code users} table. An internal detail of {@link JpaUserRepository},
 * never the domain port itself.
 */
interface UserJpaData extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
