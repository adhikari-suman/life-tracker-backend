# Reporting is computed on demand, per currency, with transfers excluded by construction

Net worth, spending, and income are **read-side computations over postings** (ADR-0004), never stored
figures. Net worth is Assets − Liabilities; spending is Expense-account activity over a date range;
income is Income-account activity. All three are broken down **per currency** — valuing across
currencies into one number needs a base currency and historical rates, which ADR-0002 puts out of
scope. Transfers, lends, and repayments never touch an Income or Expense account, so they are absent
from spending and income **by construction** (ADR-0001), with no filter to remember.

## Why this shape

- **Computed, never closed.** ADR-0004 already decided the books never close and net worth is derived
  on demand. Reporting is the read side of that: a `ReportQueryService` that sums postings, joined to
  accounts for the kind and currency and to transactions for the date. Nothing is precomputed or
  cached; correctness is a `GROUP BY`, not a maintained total.
- **Per currency, honestly.** A USD balance and a GBP balance are both real money; summing them needs
  an exchange rate this slice deliberately does not have (ADR-0002). So net worth returns a figure
  *per currency* — `{USD: …, GBP: …}` — rather than a single number that would be a lie. A later slice
  can add base-currency valuation on top without changing what is recorded.
- **Receivables and payables land where they belong.** A receivable is an Asset account and a payable
  a Liability account (no new kind); they flow into net worth and stay out of spending and income.
  Lending a friend money is not spending, being repaid is not income — the account kinds enforce that,
  so the reports never double-count a shared bill or a loan.
- **Breakdown by account.** Spending and income return per-account amounts (Groceries £200, Rent
  £1000), not just a grand total, because "where did it go?" is the question. The per-currency totals
  ride alongside.

## Consequences

- Spending on an Expense account over a range is its net debit movement there (`Σdebit − Σcredit`,
  so a refund reduces it); income on an Income account is `Σcredit − Σdebit`. Both filter by the
  transaction's date, inclusive, with an open range meaning all time.
- The reports are owner-scoped query services (ADR-0006). No new write model, no new table.
- Cross-currency net worth (one number across currencies) and richer breakdowns (by merchant, by
  tag) ride later slices — the first needs ADR-0002's base currency, the second the deferred metadata.
