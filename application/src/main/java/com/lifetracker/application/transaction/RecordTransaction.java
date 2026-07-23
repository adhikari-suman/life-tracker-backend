package com.lifetracker.application.transaction;

import com.lifetracker.application.labeling.LabelArchivedException;
import com.lifetracker.application.labeling.LabelNotApplicableException;
import com.lifetracker.application.labeling.LabelNotFoundException;
import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.labeling.Label;
import com.lifetracker.domain.labeling.LabelId;
import com.lifetracker.domain.labeling.LabelRepository;
import com.lifetracker.domain.labeling.PostingLabelRepository;
import com.lifetracker.domain.money.CurrencyMismatchException;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.Posting;
import com.lifetracker.domain.transaction.PostingId;
import com.lifetracker.domain.transaction.Transaction;
import com.lifetracker.domain.transaction.TransactionId;
import com.lifetracker.domain.transaction.TransactionRepository;

import java.util.List;

/**
 * Records a movement as a balanced transaction (ADR-0012): money leaves the {@code from} account
 * (credited) and arrives in {@code to} (debited). A same-currency movement carries one {@code amount};
 * a cross-currency one carries a second real amount for the destination leg, and the rate is derived,
 * never multiplied (ADR-0002). The account kinds make the result correct without the caller thinking
 * in debits and credits (ADR-0001).
 *
 * <p>An optional label is metadata attached beside the ledger, never a field of it (ADR-0014). The
 * caller does not say which posting it belongs to: it goes on whichever leg is the Income or Expense
 * account, because that is the only leg where money entered or left your world. A movement between
 * accounts you hold — a transfer, a debt payment, an opening balance — has no such leg, and a label
 * sent with one is refused rather than quietly dropped.
 */
public final class RecordTransaction {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final LabelRepository labels;
    private final PostingLabelRepository postingLabels;

    public RecordTransaction(AccountRepository accounts, TransactionRepository transactions,
                             LabelRepository labels, PostingLabelRepository postingLabels) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.labels = labels;
        this.postingLabels = postingLabels;
    }

    public TransactionId execute(RecordTransactionCommand command) {
        if (command.from().equals(command.to())) {
            throw new SameAccountException();
        }
        Account from = accounts.findById(command.owner(), command.from()).orElseThrow(UnknownAccountException::new);
        Account to = accounts.findById(command.owner(), command.to()).orElseThrow(UnknownAccountException::new);

        Money fromLeg = command.amount();
        if (!fromLeg.currency().equals(from.currency())) {
            throw new CurrencyMismatchException(from.currency(), fromLeg.currency());
        }

        Money toLeg;
        if (command.toAmount() != null) {
            toLeg = command.toAmount();
            if (!toLeg.currency().equals(to.currency())) {
                throw new CurrencyMismatchException(to.currency(), toLeg.currency());
            }
        } else if (from.currency().equals(to.currency())) {
            toLeg = fromLeg;
        } else {
            throw new ConvertedAmountRequiredException();
        }

        Posting credit = Posting.credit(command.from(), fromLeg);
        Posting debit = Posting.debit(command.to(), toLeg);

        // Resolved BEFORE the transaction is saved, so a label that cannot be applied never leaves a
        // recorded-but-miscategorized transaction behind.
        PostingId labelTarget = command.labelId() == null
                ? null
                : resolveLabelTarget(command, from, to, credit, debit);

        // Same-currency legs must be equal (the aggregate's balance check enforces it, and throws
        // UnbalancedTransactionException on a mismatch); cross-currency legs are balanced by the
        // derived rate and skip the equality check (ADR-0002).
        Transaction transaction = Transaction.record(TransactionId.generate(), command.date(), List.of(credit, debit));
        transactions.save(command.owner(), transaction);

        if (labelTarget != null) {
            postingLabels.assign(command.owner(), labelTarget, LabelId.of(command.labelId()));
        }
        return transaction.id();
    }

    /** The leg a label belongs on: the movement's one boundary account, when there is exactly one. */
    private PostingId resolveLabelTarget(RecordTransactionCommand command, Account from, Account to,
                                         Posting credit, Posting debit) {
        Label label = labels.findById(command.owner(), LabelId.of(command.labelId()))
                .orElseThrow(LabelNotFoundException::new);
        if (label.isArchived()) {
            throw new LabelArchivedException();
        }

        boolean fromIsBoundary = from.kind().isBoundary();
        boolean toIsBoundary = to.kind().isBoundary();

        if (fromIsBoundary && toIsBoundary) {
            // Income straight into Expense: legal double-entry, but two legs could hold the category
            // and one label cannot say which. Refused rather than guessed; splits will settle it.
            throw new LabelNotApplicableException(
                    "both legs are boundary accounts, so one label cannot say which of them it describes");
        }
        if (!fromIsBoundary && !toIsBoundary) {
            throw new LabelNotApplicableException(
                    "this movement is between accounts you hold (" + from.kind() + " to " + to.kind()
                            + "), so nothing entered or left your world and there is nothing to categorize");
        }
        return fromIsBoundary ? credit.id() : debit.id();
    }
}
