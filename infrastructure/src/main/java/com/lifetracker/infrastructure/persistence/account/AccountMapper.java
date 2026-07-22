package com.lifetracker.infrastructure.persistence.account;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.account.AccountName;
import com.lifetracker.domain.ledger.OwnerId;

import java.util.Currency;

/** Converts between the domain {@link Account} (no owner) and the {@link AccountEntity} (owner-stamped). */
final class AccountMapper {

    private AccountMapper() {
    }

    static AccountEntity toEntity(OwnerId owner, Account account) {
        return new AccountEntity(
                account.id().value(),
                owner.value(),
                account.name().value(),
                account.kind().name(),
                account.currency().getCurrencyCode());
    }

    static Account toDomain(AccountEntity entity) {
        return Account.rehydrate(
                AccountId.of(entity.getId()),
                new AccountName(entity.getName()),
                AccountKind.valueOf(entity.getKind()),
                Currency.getInstance(entity.getCurrency()));
    }
}
