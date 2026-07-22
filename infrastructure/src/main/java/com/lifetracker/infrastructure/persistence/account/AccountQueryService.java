package com.lifetracker.infrastructure.persistence.account;

import com.lifetracker.domain.account.AccountId;
import com.lifetracker.domain.account.AccountKind;
import com.lifetracker.domain.ledger.EntrySide;
import com.lifetracker.domain.ledger.OwnerId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Read side for accounts. Each view carries a balance computed on demand (ADR-0004): the signed sum of
 * its postings on its kind's normal side, so a bank in credit and a credit card's owed amount both read
 * positive, an overdraft reads negative. Owner-scoped ({@link OwnerId}) — the ADR-0006 read guard.
 */
@Component
public class AccountQueryService {

    private final AccountJpaData data;
    private final EntityManager entityManager;

    AccountQueryService(AccountJpaData data, EntityManager entityManager) {
        this.data = data;
        this.entityManager = entityManager;
    }

    public List<AccountView> findByOwner(OwnerId owner) {
        List<AccountEntity> accounts = data.findByOwnerIdOrderByName(owner.value());
        Map<UUID, BigDecimal> balances = balancesFor(accounts);
        return accounts.stream().map(account -> toView(account, balances)).toList();
    }

    public Optional<AccountView> findById(OwnerId owner, AccountId id) {
        return data.findByOwnerIdAndId(owner.value(), id.value())
                .map(account -> toView(account, balancesFor(List.of(account))));
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, BigDecimal> balancesFor(List<AccountEntity> accounts) {
        if (accounts.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = accounts.stream().map(AccountEntity::getId).toList();
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select account_id, side, sum(amount) from postings where account_id in (:ids) "
                                + "group by account_id, side")
                .setParameter("ids", ids)
                .getResultList();

        Map<UUID, BigDecimal> debits = new HashMap<>();
        Map<UUID, BigDecimal> credits = new HashMap<>();
        for (Object[] row : rows) {
            UUID accountId = (UUID) row[0];
            BigDecimal sum = (BigDecimal) row[2];
            ("DEBIT".equals(row[1]) ? debits : credits).put(accountId, sum);
        }

        Map<UUID, BigDecimal> balances = new HashMap<>();
        for (AccountEntity account : accounts) {
            BigDecimal debit = debits.getOrDefault(account.getId(), BigDecimal.ZERO);
            BigDecimal credit = credits.getOrDefault(account.getId(), BigDecimal.ZERO);
            EntrySide normal = AccountKind.valueOf(account.getKind()).normalSide();
            balances.put(account.getId(), normal == EntrySide.DEBIT ? debit.subtract(credit) : credit.subtract(debit));
        }
        return balances;
    }

    private AccountView toView(AccountEntity account, Map<UUID, BigDecimal> balances) {
        BigDecimal balance = balances.getOrDefault(account.getId(), BigDecimal.ZERO)
                .setScale(4, RoundingMode.UNNECESSARY);
        return new AccountView(account.getId(), account.getName(), account.getKind(), account.getCurrency(), balance);
    }
}
