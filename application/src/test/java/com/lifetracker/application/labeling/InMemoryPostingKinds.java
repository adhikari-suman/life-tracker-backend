package com.lifetracker.application.labeling;

import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.labeling.PostingKinds;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** In-memory {@link PostingKinds} fake — the account kind behind each posting, stated up front. */
public final class InMemoryPostingKinds implements PostingKinds {

    private record Key(OwnerId owner, PostingId posting) {
    }

    private final Map<Key, AccountKind> store = new LinkedHashMap<>();

    public void put(OwnerId owner, PostingId posting, AccountKind kind) {
        store.put(new Key(owner, posting), kind);
    }

    @Override
    public Optional<AccountKind> kindOf(OwnerId owner, PostingId posting) {
        return Optional.ofNullable(store.get(new Key(owner, posting)));
    }
}
