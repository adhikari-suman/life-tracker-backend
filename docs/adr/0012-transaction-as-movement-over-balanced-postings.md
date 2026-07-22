# A transaction is recorded as a movement (from → to) but stored as balanced double-entry postings

The write API for the ledger is a **movement**: `{ date, from, to, amount }`. Money leaves the `from`
account and arrives in the `to` account; the backend books it as a balanced pair of postings —
**credit `from`, debit `to`** — and the account kinds (ADR-0001) make that correct without the user
ever thinking in debits and credits. Under the hood the aggregate is a `Transaction` holding a **list
of postings** with the invariant that debits equal credits within a currency, so the model is full
double-entry from day one; the movement is just the honest wire shape for the common two-account case.

## Why this shape

- **A personal ledger must not demand accounting literacy.** "Moved £200 from bank to cash", "spent
  £50 from bank on groceries", "salary landed in bank" are all one shape — `from → to` — and the
  Dr/Cr falls out of the kinds (credit the source, debit the destination). Asking the user to label
  each leg debit or credit pushes the ledger's internals onto them for no gain.
- **Store postings, not the movement.** The movement is a convenience; the truth is the postings.
  Keeping `Transaction` a balanced set of N postings — never a fixed pair — means the next step,
  **splits**, is the same aggregate with a richer wire form, not a rewrite. A shared restaurant bill
  I front for three friends is one credit of £80 from my account against four debits: £20 to an
  Expense (my share) and £20 to each friend's Receivable. Because a Receivable is an Asset (ADR-0003),
  the £60 owed to me is not spending — my "spent at the restaurant" is £20, exactly right. That is
  ADR-0001 paying off, and the aggregate already holds it.
- **The invariant lives in the aggregate.** `Transaction` refuses to exist unless its postings balance
  (Σdebit = Σcredit) within a currency (ADR-0002). The movement API can only produce a balanced pair,
  but the guard is on the aggregate, so the split API inherits it for free.
- **Balances stay computed.** An account's balance and net worth are read-side sums over postings,
  never stored (ADR-0004). Posting amounts are non-negative `Money`; a balance may be negative, so it
  is a signed read-model value, not a domain `Money`.

## Considered options

- **Raw postings on the wire** (`postings: [{account, debit|credit, amount}]`). Rejected as the
  primary shape: faithful, but it makes the client speak double-entry. It is the form the split API
  will generalize to, and the transaction *response* already returns postings, so a client that wants
  the raw view has it.
- **A fixed two-posting transaction.** Rejected: modelling the movement literally would force a
  rewrite when splits land. N postings from the start costs nothing now.

## Consequences

- Slice 1 is single-currency movements (from and to share a currency); a cross-currency movement is
  refused (`CROSS_CURRENCY_UNSUPPORTED`) until ADR-0002's two-real-amounts recording is built.
- Opening balances need no special mechanism: they are a movement from an Equity account into the new
  account (ADR-0004), so they fall out of the same API.
- The transaction response is postings-shaped (the ledger truth); a two-account movement is just the
  two-posting case a client renders as "from → to".
- Splits, transaction edit/reversal, and metadata (merchant, tags, even a free-text note) are
  deliberately later slices; the aggregate is shaped so they attach without disturbing the core.
