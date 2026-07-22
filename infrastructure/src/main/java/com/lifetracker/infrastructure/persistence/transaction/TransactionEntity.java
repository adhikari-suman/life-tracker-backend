package com.lifetracker.infrastructure.persistence.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The {@code transactions} table (code-first, ADR-0009). The header of a transaction: its owner and
 * date. The balanced postings live in the {@code postings} table ({@link PostingEntity}). Immutable.
 * Package-private.
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

    // Null for a same-currency transaction; the derived rate for a cross-currency one (ADR-0002).
    @Column(name = "exchange_rate", precision = 19, scale = 8, updatable = false)
    private BigDecimal exchangeRate;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected TransactionEntity() {
    }

    TransactionEntity(UUID id, UUID ownerId, LocalDate date, BigDecimal exchangeRate) {
        this.id = id;
        this.ownerId = ownerId;
        this.date = date;
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

    BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
