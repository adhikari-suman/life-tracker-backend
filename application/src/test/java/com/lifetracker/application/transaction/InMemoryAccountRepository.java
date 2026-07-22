package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.ledger.OwnerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory owner-scoped {@link AccountRepository} fake for the transaction use-case tests. */
final class InMemoryAccountRepository implements AccountRepository {

    private record Key(OwnerId owner, AccountId id) {
    }

    private final Map<Key, Account> store = new HashMap<>();

    @Override
    public void save(OwnerId owner, Account account) {
        store.put(new Key(owner, account.id()), account);
    }

    @Override
    public Optional<Account> findById(OwnerId owner, AccountId id) {
        return Optional.ofNullable(store.get(new Key(owner, id)));
    }
}
