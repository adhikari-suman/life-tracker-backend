package com.lifetracker.domain.account;

import com.lifetracker.domain.ledger.OwnerId;

import java.util.Optional;

/**
 * The store of accounts, owner-scoped. The owner is passed in on every call and never held on the
 * aggregate — isolation lives around the Ledger (ADR-0006); the adapter stamps and filters by
 * {@code owner_id}. The port speaks Ledger domain types only, never a User.
 */
public interface AccountRepository {

    void save(OwnerId owner, Account account);

    Optional<Account> findById(OwnerId owner, AccountId id);
}
