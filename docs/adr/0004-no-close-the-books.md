# The books never close; Equity is opening capital only, and net worth is computed on demand

This is a personal ledger, not an accounting system for tax filing. So we deliberately do
**not** close the books. Income and Expense accounts accumulate and are never swept into
Equity at period end; there are no closing entries and no Retained Earnings. **Equity holds
only opening capital** — the balance an account already carried the day it entered the ledger,
recorded as an ordinary balanced transaction against Equity so it counts as neither income nor
a transfer.

Net worth is **computed on demand as Assets − Liabilities**, never stored as a closed Equity
figure. Spending and earning over any window are **date-range queries over postings**, not
reads of an account balance.

## Why this is written down

A developer who knows double-entry will look at this model and see a gap: no period close, no
Retained Earnings, temporary accounts that never reset. That absence is a **decision, not an
oversight**. Full close-the-books was considered and rejected — it forces a defined accounting
period, turns Income/Expense into temporary accounts that reset each period, and generates
system closing entries that then have to be filtered out of every report — all to store a
number (net worth, surplus) that is trivial to compute from Assets, Liabilities, and dated
postings. The machinery costs more than the number is worth here.

## Consequences

- Income/Expense balances are cumulative, not per-period; there is no "start of year zero".
- If this ever grows into something that genuinely needs closed periods (formal reporting,
  multi-year retained earnings as stored fact), this is the decision to revisit first.
