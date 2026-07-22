package com.lifetracker.application.account;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.account.AccountName;
import com.lifetracker.domain.account.AccountRepository;

import java.util.Currency;

/**
 * Opens a new account in the caller's Book: validate the name, kind, and currency, then store it
 * owner-scoped. Returns the new {@link AccountId}. Orchestrates; the invariants live in the value
 * objects and the aggregate.
 */
public final class OpenAccount {

    private final AccountRepository accounts;

    public OpenAccount(AccountRepository accounts) {
        this.accounts = accounts;
    }

    public AccountId execute(OpenAccountCommand command) {
        AccountName name = new AccountName(command.name());       // InvalidAccountNameException -> 422
        AccountKind kind = parseKind(command.kind());             // InvalidAccountException      -> 422
        Currency currency = parseCurrency(command.currency());    // InvalidAccountException      -> 422

        Account account = Account.open(AccountId.generate(), name, kind, currency);
        accounts.save(command.owner(), account);
        return account.id();
    }

    private static AccountKind parseKind(String kind) {
        try {
            return AccountKind.valueOf(kind);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidAccountException("unknown account kind: " + kind);
        }
    }

    private static Currency parseCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidAccountException("unknown currency: " + code);
        }
    }
}
