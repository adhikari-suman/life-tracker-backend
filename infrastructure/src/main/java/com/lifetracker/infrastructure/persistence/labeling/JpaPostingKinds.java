package com.lifetracker.infrastructure.persistence.labeling;

import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.labeling.PostingKinds;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.PostingId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * The {@link PostingKinds} port: the kind of account a posting was made to.
 *
 * <p>Owner-scoped through the account, since {@code postings} carries no {@code owner_id} of its own —
 * a posting belongs to whoever owns the account it touches. A posting in another Book is simply not
 * found, which is what keeps a label from ever reaching across a tenant boundary (ADR-0006).
 */
@Component
class JpaPostingKinds implements PostingKinds {

    private final EntityManager entityManager;

    JpaPostingKinds(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<AccountKind> kindOf(OwnerId owner, PostingId posting) {
        List<String> kinds = entityManager.createNativeQuery(
                        "select a.kind from postings p join accounts a on a.id = p.account_id "
                                + "where p.id = :posting and a.owner_id = :owner")
                .setParameter("posting", posting.value())
                .setParameter("owner", owner.value())
                .getResultList();
        return kinds.stream().findFirst().map(AccountKind::valueOf);
    }
}
