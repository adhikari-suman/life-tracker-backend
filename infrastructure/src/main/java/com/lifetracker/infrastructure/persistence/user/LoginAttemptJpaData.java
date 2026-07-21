package com.lifetracker.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data access to {@code login_attempts}. An internal detail of {@link JpaLoginAttempts},
 * never the domain port itself.
 */
interface LoginAttemptJpaData extends JpaRepository<LoginAttemptEntity, UUID> {

    @Query("select a.attemptedAt from LoginAttemptEntity a where a.email = :email and a.attemptedAt >= :cutoff")
    List<Instant> attemptedAtByEmailSince(@Param("email") String email, @Param("cutoff") Instant cutoff);

    void deleteByEmail(String email);

    void deleteByEmailAndAttemptedAtLessThan(String email, Instant cutoff);
}
