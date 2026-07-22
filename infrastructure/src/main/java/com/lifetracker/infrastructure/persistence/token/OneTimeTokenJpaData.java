package com.lifetracker.infrastructure.persistence.token;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data access to {@code one_time_tokens}. Internal to {@link JpaOneTimeTokenRepository}. */
interface OneTimeTokenJpaData extends JpaRepository<OneTimeTokenEntity, UUID> {

    Optional<OneTimeTokenEntity> findByTokenHash(String tokenHash);

    void deleteByUserIdAndPurpose(UUID userId, String purpose);
}
