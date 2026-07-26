package com.lifetracker.infrastructure.persistence.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code transactions} table (code-first, ADR-0009). The header of a transaction: its owner,
 * and when the money moved. The balanced postings live in the {@code postings} table
 * ({@link PostingEntity}). Immutable. Package-private.
 *
 * <p>Two different moments live here and must not be confused. {@code tx_date} and {@code tx_time}
 * are <em>Occurred At</em>: a wall-clock reading supplied by the person recording it, stored
 * zoneless so no conversion can move a late-evening purchase into the next day (ADR-0018).
 * {@code created_at} is <em>Recorded At</em>: an instant the system observed, in UTC. Only the
 * date is ever grouped by in reporting.
 */
@Entity
@Table(name = "transactions")
class TransactionEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "tx_date", nullable = false, updatable = false)
    private LocalDate date;

    // LocalTime, never OffsetTime: an offset is exactly what this value must not carry (ADR-0018).
    @Column(name = "tx_time", nullable = false, updatable = false)
    private LocalTime time;

    // Null for a same-currency transaction; the derived rate for a cross-currency one (ADR-0002).
    @Column(name = "exchange_rate", precision = 19, scale = 8, updatable = false)
    private BigDecimal exchangeRate;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TransactionEntity() {
    }

    TransactionEntity(UUID id, UUID ownerId, LocalDate date, LocalTime time, BigDecimal exchangeRate) {
        this.id = id;
        this.ownerId = ownerId;
        this.date = date;
        this.time = time;
        this.exchangeRate = exchangeRate;
    }

    UUID getId() {
        return id;
    }

    UUID getOwnerId() {
        return ownerId;
    }

    LocalDate getDate() {
        return date;
    }

    LocalTime getTime() {
        return time;
    }

    BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
