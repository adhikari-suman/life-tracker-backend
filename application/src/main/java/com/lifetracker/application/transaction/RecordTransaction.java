package com.lifetracker.application.transaction;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.money.CurrencyMismatchException;
import com.lifetracker.domain.transaction.Posting;
import com.lifetracker.domain.transaction.Transaction;
import com.lifetracker.domain.transaction.TransactionId;
import com.lifetracker.domain.transaction.TransactionRepository;

import java.util.List;

/**
 * Records a movement as a balanced transaction (ADR-0012): money leaves the {@code from} account
 * (credited) and arrives in {@code to} (debited). Both accounts must belong to the caller, must
 * differ, and must share the amount's currency — cross-currency is a later slice. The account kinds
 * make the result correct without the caller thinking in debits and credits (ADR-0001).
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
        if (!from.currency().equals(to.currency())) {
            throw new CrossCurrencyUnsupportedException();
        }
        if (!command.amount().currency().equals(from.currency())) {
            throw new CurrencyMismatchException(from.currency(), command.amount().currency());
        }

        Transaction transaction = Transaction.record(TransactionId.generate(), command.date(), List.of(
                Posting.credit(command.from(), command.amount()),
                Posting.debit(command.to(), command.amount())));
        transactions.save(command.owner(), transaction);
        return transaction.id();
    }
}
