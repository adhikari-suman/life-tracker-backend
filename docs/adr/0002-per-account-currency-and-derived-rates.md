# Each account has a fixed home currency; cross-currency transactions record both real amounts and derive the rate

Every account holds exactly one currency, and every posting to it is in that currency. A
transaction may span two currencies (moving money between a GBP account and a USD account, or
a foreign purchase). When it does, each leg carries its own real amount — the figure copied
from that account's real statement — and the exchange rate is *derived* from the two amounts
(an £80 leg and a $100 leg imply 1.25) and stored on the transaction for reference. The rate
is never multiplied by anything to produce an amount.

We chose this over the two obvious alternatives:

- **A single ledger currency with foreign amounts as annotation.** Rejected because the user
  genuinely holds accounts in more than one currency; a USD balance is real money, not a
  display note on a GBP figure.
- **Rate as an input that computes the second leg.** Rejected because it manufactures a
  rounding problem: a computed leg (£80 × 1.2734 = $101.87) may disagree with what the second
  account's statement actually says by a cent, forcing tolerances and residual-rounding
  accounts. Since both amounts are already known facts from two real statements, we record
  both and let the rate fall out — so there is nothing to reconcile.

## Consequences

- The strict "debits equal credits" check applies exactly *within a single currency*. Across
  currencies the two sides are equal by value at the derived rate, which holds by construction
  — so cross-currency transactions do not get the same typo-catching that same-currency ones
  do. Acceptable: the figures are copied from real statements, and single-currency
  transactions (the vast majority) keep the strict check.
- Valuing net worth *across* currencies (GBP + USD as one number) is a separate reporting
  concern that needs a base currency and historical rates. It is deliberately out of scope
  here; this decision is only about recording transactions correctly.
