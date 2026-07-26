package com.lifetracker.infrastructure.persistence.transaction;

import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.domain.transaction.Posting;
import com.lifetracker.domain.transaction.Transaction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** Converts the domain {@link Transaction} (no owner) into its owner-stamped header + posting rows. */
final class TransactionMapper {

    private TransactionMapper() {
    }

    static TransactionEntity toEntity(OwnerId owner, Transaction transaction) {
        return new TransactionEntity(transaction.id().value(), owner.value(), transaction.date(),
                transaction.time(), deriveExchangeRate(transaction));
    }

    /**
     * The rate for reference on a cross-currency movement: the destination (debit) amount over the
     * source (credit) amount, to 8 places (ADR-0002). Null for a same-currency transaction or a split.
     * Derived here at the boundary, so no {@code BigDecimal} leaks into the domain (it stays inside Money).
     */
    private static BigDecimal deriveExchangeRate(Transaction transaction) {
        List<Posting> postings = transaction.postings();
        if (postings.size() != 2) {
            return null;
        }
        Posting a = postings.get(0);
        Posting b = postings.get(1);
        if (a.side() == b.side() || a.amount().currency().equals(b.amount().currency())) {
            return null;
        }
        Posting credit = a.side() == EntrySide.CREDIT ? a : b;
        Posting debit = a.side() == EntrySide.DEBIT ? a : b;
        if (credit.amount().amount().signum() == 0) {
            return null;
        }
        return debit.amount().amount().divide(credit.amount().amount(), 8, RoundingMode.HALF_UP);
    }

    /**
     * The posting's id comes from the domain, not from here: a label is keyed by it (ADR-0014), so the
     * use case must know it at the moment it records the movement rather than discovering it after a
     * round trip to the database.
     */
    static List<PostingEntity> toPostingEntities(Transaction transaction) {
        return transaction.postings().stream()
                .map(posting -> new PostingEntity(
                        posting.id().value(),
                        transaction.id().value(),
                        posting.accountId().value(),
                        posting.side().name(),
                        posting.amount().amount(),
                        posting.amount().currency().getCurrencyCode()))
                .toList();
    }
}
