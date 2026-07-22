package com.lifetracker.infrastructure.persistence.transaction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The {@code postings} table (code-first, ADR-0009): one leg of a transaction — a debit or credit of a
 * non-negative amount to an account. The money is a {@code NUMERIC(19,4)} amount plus its currency
 * (ADR-0002); the {@code side} carries the direction. Immutable. Package-private.
 */
@Entity
@Table(name = "postings")
class PostingEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "transaction_id", nullable = false, updatable = false)
    private UUID transactionId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private UUID accountId;

    @Column(name = "side", nullable = false, updatable = false, length = 6)
    private String side;

    @Column(name = "amount", nullable = false, updatable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, updatable = false, length = 3)
    private String currency;

    protected PostingEntity() {
    }

    PostingEntity(UUID id, UUID transactionId, UUID accountId, String side, BigDecimal amount, String currency) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.side = side;
        this.amount = amount;
        this.currency = currency;
    }

    UUID getId() {
        return id;
    }

    UUID getTransactionId() {
        return transactionId;
    }

    UUID getAccountId() {
        return accountId;
    }

    String getSide() {
        return side;
    }

    BigDecimal getAmount() {
        return amount;
    }

    String getCurrency() {
        return currency;
    }
}
