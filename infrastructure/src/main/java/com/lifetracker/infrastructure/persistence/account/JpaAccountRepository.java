package com.lifetracker.infrastructure.persistence.account;

import com.lifetracker.domain.account.Account;
import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountRepository;
import com.lifetracker.domain.ledger.OwnerId;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** The {@link AccountRepository} port, backed by Spring Data JPA. Owner-scoped; speaks Ledger types. */
@Repository
class JpaAccountRepository implements AccountRepository {

    private final AccountJpaData data;

    JpaAccountRepository(AccountJpaData data) {
        this.data = data;
    }

    @Override
    public void save(OwnerId owner, Account account) {
        data.save(AccountMapper.toEntity(owner, account));
    }

    @Override
    public Optional<Account> findById(OwnerId owner, AccountId id) {
        return data.findByOwnerIdAndId(owner.value(), id.value()).map(AccountMapper::toDomain);
    }
}
