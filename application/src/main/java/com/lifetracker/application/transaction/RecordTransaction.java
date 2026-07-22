package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.money.CurrencyMismatchException;
import com.lifetracker.domain.money.Money;
import com.lifetracker.domain.transaction.Posting;
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
 */
public final class RecordTransaction {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public RecordTransaction(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
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

        // Same-currency legs must be equal (the aggregate's balance check enforces it, and throws
        // UnbalancedTransactionException on a mismatch); cross-currency legs are balanced by the
        // derived rate and skip the equality check (ADR-0002).
        Transaction transaction = Transaction.record(TransactionId.generate(), command.date(), List.of(
                Posting.credit(command.from(), fromLeg),
                Posting.debit(command.to(), toLeg)));
        transactions.save(command.owner(), transaction);
        return transaction.id();
    }
}
