package com.lifetracker.infrastructure.persistence.report;

import com.lifetracker.domain.ledger.OwnerId;
import com.lifetracker.infrastructure.persistence.labeling.LabelQueryService;
import com.lifetracker.infrastructure.persistence.labeling.LabelView;
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

    /**
     * The name of the row that carries postings with no label. Not a label anyone creates or applies —
     * it is the name for the remainder, so a breakdown always accounts for every penny.
     */
    private static final String UNCATEGORIZED = "Uncategorized";

    private final EntityManager entityManager;
    private final LabelQueryService labelQuery;

    ReportQueryService(EntityManager entityManager, LabelQueryService labelQuery) {
        this.entityManager = entityManager;
        this.labelQuery = labelQuery;
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

    public List<LabelAmountView> spendingByLabel(OwnerId owner, LocalDate from, LocalDate to) {
        return activityByLabel(owner, "EXPENSE", "DEBIT", from, to);
    }

    public List<LabelAmountView> incomeByLabel(OwnerId owner, LocalDate from, LocalDate to) {
        return activityByLabel(owner, "INCOME", "CREDIT", from, to);
    }

    /**
     * The same money as {@link #activity}, sliced by what it was FOR instead of where it was booked
     * (ADR-0014). Because it sums the identical postings over the identical range, the two breakdowns
     * must reconcile: the sum of every row's {@code own} equals the per-account total for that
     * currency.
     *
     * <p>The LEFT JOIN is what makes that true. Postings with no label fall into the {@code null}
     * group, which becomes the Uncategorized row — so nothing is silently dropped from the breakdown
     * just because it was never categorized.
     *
     * <p>Roll-up is computed here rather than in SQL. The tree is capped at three levels and a personal
     * Book holds dozens of labels, so walking it in memory is both cheaper and far easier to read than
     * a recursive CTE — and it keeps the roll-up rule in one place.
     */
    @SuppressWarnings("unchecked")
    private List<LabelAmountView> activityByLabel(OwnerId owner, String kind, String normalSide,
                                                  LocalDate from, LocalDate to) {
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select pl.label_id, a.currency, "
                                + "sum(case when p.side = :normal then p.amount else -p.amount end) "
                                + "from accounts a "
                                + "join postings p on p.account_id = a.id "
                                + "join transactions t on t.id = p.transaction_id "
                                + "left join posting_labels pl on pl.posting_id = p.id "
                                + "where a.owner_id = :owner and a.kind = :kind and t.tx_date between :from and :to "
                                + "group by pl.label_id, a.currency")
                .setParameter("owner", owner.value())
                .setParameter("kind", kind)
                .setParameter("normal", normalSide)
                .setParameter("from", from != null ? from : OPEN_START)
                .setParameter("to", to != null ? to : OPEN_END)
                .getResultList();

        // Archived labels are included on purpose: retiring a label hides it from the picker but must
        // never hide the history already tagged with it (ADR-0015).
        List<LabelView> labels = labelQuery.findByOwner(owner, true);
        Map<UUID, LabelView> byId = new HashMap<>();
        for (LabelView label : labels) {
            byId.put(label.id(), label);
        }

        Map<UUID, Map<String, BigDecimal>> own = new HashMap<>();
        Map<UUID, Map<String, BigDecimal>> rolledUp = new HashMap<>();
        Map<String, BigDecimal> uncategorized = new HashMap<>();

        for (Object[] row : rows) {
            UUID labelId = (UUID) row[0];
            String currency = (String) row[1];
            BigDecimal amount = (BigDecimal) row[2];
            if (labelId == null || !byId.containsKey(labelId)) {
                uncategorized.merge(currency, amount, BigDecimal::add);
                continue;
            }
            own.computeIfAbsent(labelId, key -> new HashMap<>()).merge(currency, amount, BigDecimal::add);
            for (UUID ancestor : selfAndAncestors(labelId, byId)) {
                rolledUp.computeIfAbsent(ancestor, key -> new HashMap<>()).merge(currency, amount, BigDecimal::add);
            }
        }

        List<LabelAmountView> result = new ArrayList<>();
        for (LabelView label : labels) {
            Map<String, BigDecimal> ownByCurrency = own.getOrDefault(label.id(), Map.of());
            Map<String, BigDecimal> rolledByCurrency = rolledUp.getOrDefault(label.id(), Map.of());
            // A row appears when EITHER figure is non-zero, so a parent with no direct spending still
            // shows the total sitting underneath it.
            Set<String> currencies = new TreeSet<>(ownByCurrency.keySet());
            currencies.addAll(rolledByCurrency.keySet());
            for (String currency : currencies) {
                BigDecimal ownAmount = ownByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                BigDecimal rolledAmount = rolledByCurrency.getOrDefault(currency, BigDecimal.ZERO);
                if (ownAmount.signum() == 0 && rolledAmount.signum() == 0) {
                    continue;
                }
                result.add(new LabelAmountView(label.id(), label.name(), label.path(), label.parentId(),
                        currency, scaled(ownAmount), scaled(rolledAmount)));
            }
        }
        for (String currency : new TreeSet<>(uncategorized.keySet())) {
            BigDecimal amount = uncategorized.get(currency);
            if (amount.signum() == 0) {
                continue;
            }
            // No label id, and own == rolledUp: nothing nests beneath the absence of a label.
            result.add(new LabelAmountView(null, UNCATEGORIZED, UNCATEGORIZED, null,
                    currency, scaled(amount), scaled(amount)));
        }
        return result;
    }

    /** The label and every ancestor above it — the chain a posting's amount rolls up through. */
    private static List<UUID> selfAndAncestors(UUID labelId, Map<UUID, LabelView> byId) {
        List<UUID> chain = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        UUID current = labelId;
        while (current != null && seen.add(current)) {
            LabelView label = byId.get(current);
            if (label == null) {
                break;
            }
            chain.add(current);
            current = label.parentId();
        }
        return chain;
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
