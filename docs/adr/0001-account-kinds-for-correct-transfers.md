# Classify every account by kind, so internal transfers are correct by construction

Every account carries one of five kinds — Asset, Liability, Income, Expense, Equity. Asset
and Liability are accounts the user owns (net worth); Income and Expense are the boundaries
where money enters and leaves; Equity holds opening balances. We chose this because the whole
reason this ledger exists is to get **internal transfers** right, and the account kind is what
makes that structural rather than a matter of remembering.

Moving £200 from a bank account to cash is one transaction: credit the bank, debit cash. Both
accounts are Asset, so the transaction touches no Income or Expense account. Every spending
question sums Expense accounts and every earning question sums Income accounts, so a transfer
is excluded from both **automatically** — it was never earning or spending, and the model
knows that without being told. Consumer apps (Mint, YNAB, and the like) book the two legs as
£200 spent plus £200 earned, spiking both charts with money that never left the user's
control; fixing that is the point of the product, not a nice-to-have.

The account kind also fixes the debit/credit normal side (Asset and Expense grow on debit;
Liability, Income, Equity on credit), so classifying an account is the only thing set by hand
— the Dr/Cr behaviour follows.

## Considered options

- **A manual "internal transfer" flag the user sets per transaction.** Rejected: it fails
  exactly when the user forgets, which is the failure mode we are trying to eliminate. An
  `ITR` tag still exists for easy filtering, but it is *derived and stamped* from the account
  kinds, never the source of truth.

## Consequences

- Reporting (net worth = Assets − Liabilities; spending = Expense totals; earning = Income
  totals) is defined in terms of account kind, not ad-hoc rules per query.
- Setting up an account requires choosing its kind up front; the wrong kind mis-books every
  transaction it touches, so this is a decision the UI must make hard to get wrong.
