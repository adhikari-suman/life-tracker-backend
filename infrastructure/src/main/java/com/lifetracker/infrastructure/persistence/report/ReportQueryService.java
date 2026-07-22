package com.lifetracker.infrastructure.persistence.report;

import com.lifetracker.domain.ledger.OwnerId;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * The reporting read side (ADR-0013): net worth, spending, and income summed on demand from postings,
 * joined to accounts for kind and currency and to transactions for the date. Owner-scoped
 * ({@link OwnerId}) — the ADR-0006 read guard. Per currency; transfers and repayments are excluded from
 * spending and income by construction, since they touch no Income or Expense account (ADR-0001).
 */
@Component
public class ReportQueryService {

    private static final LocalDate OPEN_START = LocalDate.of(1, 1, 1);
    private static final LocalDate OPEN_END = LocalDate.of(9999, 12, 31);

    private final EntityManager entityManager;

    ReportQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @SuppressWarnings("unchecked")
    public List<NetWorthView> netWorth(OwnerId owner) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select a.currency, a.kind, "
                                + "coalesce(sum(case when p.side = 'DEBIT' then p.amount else -p.amount end), 0) "
                                + "from accounts a left join postings p on p.account_id = a.id "
                                + "where a.owner_id = :owner and a.kind in ('ASSET', 'LIABILITY') "
                                + "group by a.currency, a.kind")
                .setParameter("owner", owner.value())
                .getResultList();

        Map<String, BigDecimal> assets = new HashMap<>();
        Map<String, BigDecimal> liabilities = new HashMap<>();
        for (Object[] row : rows) {
            String currency = (String) row[0];
            BigDecimal debitBalance = (BigDecimal) row[2];
            if ("ASSET".equals(row[1])) {
                assets.put(currency, debitBalance);          // Asset balance is the debit balance.
            } else {
                liabilities.put(currency, debitBalance.negate()); // Liability balance is credit-normal.
            }
        }

        Set<String> currencies = new HashSet<>(assets.keySet());
        currencies.addAll(liabilities.keySet());
        List<NetWorthView> result = new ArrayList<>();
        for (String currency : new TreeSet<>(currencies)) {
            BigDecimal a = scaled(assets.getOrDefault(currency, BigDecimal.ZERO));
            BigDecimal l = scaled(liabilities.getOrDefault(currency, BigDecimal.ZERO));
            result.add(new NetWorthView(currency, a, l, a.subtract(l)));
        }
        return result;
    }

    public List<AccountAmountView> spending(OwnerId owner, LocalDate from, LocalDate to) {
        return activity(owner, "EXPENSE", "DEBIT", from, to);
    }

    public List<AccountAmountView> income(OwnerId owner, LocalDate from, LocalDate to) {
        return activity(owner, "INCOME", "CREDIT", from, to);
    }

    @SuppressWarnings("unchecked")
    private List<AccountAmountView> activity(OwnerId owner, String kind, String normalSide, LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select a.id, a.name, a.currency, "
                                + "sum(case when p.side = :normal then p.amount else -p.amount end) "
                                + "from accounts a "
                                + "join postings p on p.account_id = a.id "
                                + "join transactions t on t.id = p.transaction_id "
                                + "where a.owner_id = :owner and a.kind = :kind and t.tx_date between :from and :to "
                                + "group by a.id, a.name, a.currency "
                                + "order by a.name")
                .setParameter("owner", owner.value())
                .setParameter("kind", kind)
                .setParameter("normal", normalSide)
                .setParameter("from", from != null ? from : OPEN_START)
                .setParameter("to", to != null ? to : OPEN_END)
                .getResultList();

        List<AccountAmountView> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AccountAmountView((UUID) row[0], (String) row[1], (String) row[2], scaled((BigDecimal) row[3])));
        }
        return result;
    }

    private static BigDecimal scaled(BigDecimal value) {
        return value.setScale(4, RoundingMode.UNNECESSARY);
    }
}
