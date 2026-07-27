package com.lifetracker.infrastructure.persistence.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data access to {@code transactions}. Internal to the adapter and query service.
 *
 * <p>Newest first means <em>when it happened</em>, not when it was typed: date, then the
 * wall-clock time, with {@code createdAt} surviving only to break exact ties (ADR-0018). Ordering
 * by the recording instant alone put Tuesday's dinner above Tuesday's breakfast whenever a week
 * was caught up out of order. The tiebreak is not decoration — without it two entries in the same
 * minute have no defined order and can swap places between reads.
 */
interface TransactionJpaData extends JpaRepository<TransactionEntity, UUID> {

    Optional<TransactionEntity> findByOwnerIdAndId(UUID ownerId, UUID id);

    List<TransactionEntity> findByOwnerIdOrderByDateDescTimeDescCreatedAtDesc(UUID ownerId);

    @Query("select t from TransactionEntity t where t.ownerId = :owner and t.id in "
            + "(select p.transactionId from PostingEntity p where p.accountId = :accountId) "
            + "order by t.date desc, t.time desc, t.createdAt desc")
    List<TransactionEntity> findByOwnerAndAccount(@Param("owner") UUID owner, @Param("accountId") UUID accountId);
}
