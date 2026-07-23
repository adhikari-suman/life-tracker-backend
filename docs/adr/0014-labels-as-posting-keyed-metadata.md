# A label is optional metadata attached to a posting by id, outside the ledger core

A **Label** says what money was *for*. By ADR-0003's dividing line — *if it changes a balance it is in
the ledger; if it only describes the event it is metadata* — a label is plainly metadata, so it lives
**outside** the double-entry core: the `Transaction` aggregate and its `Posting` never learn that
labels exist. A label assignment is a separate record keyed by the **posting's id**, the identity the
`postings` table already carries. That is exactly what ADR-0012 meant by metadata attaching "without
disturbing the core."

A label is **optional**, and it applies only to the leg recording money entering or leaving your world
— an Income or Expense posting. A transaction with no such leg (an internal transfer, a payment, an
opening balance) is **refused** a label rather than quietly ignoring one, because attempting to
categorize a transfer almost always means the wrong account kind was picked, and a silent no-op would
hide that. What is left untagged is **Uncategorized** — a name for the remainder in a summary, never
a label anyone assigns.

Because the assignment is metadata, **retagging is not a ledger edit**: it rewrites no posting, so the
append-only rule holds and a mis-tag is never permanent.

## Considered options

- **A label field on the `Posting` itself.** Rejected: it puts descriptive metadata inside the
  double-entry core against ADR-0003's line, and it makes retagging an edit to ledger truth — which
  append-only forbids, leaving every mis-tag permanent in a system whose entire job is categorization.
- **A system-assigned label on *every* transaction** — `ITR`, `Opening Balance`, `Credit Card
  Payment` — so nothing is ever untagged. Rejected, and this is the decision worth remembering. It
  re-opens the bug this ledger exists to close. ADR-0013 keeps transfers out of spending "by
  construction… with no filter to remember"; the moment `ITR` shares a namespace with `groceries`,
  every spending query must *remember to exclude it*, converting a structural guarantee into a
  denylist maintained forever — and one missed entry double-counts moved money as spend. It also has
  nowhere natural to sit (a transfer's two legs are both Asset; neither is "what it was for", so the
  tag would have to hang off the whole transaction, unlike every other label), and it duplicates a
  fact already derivable from account kinds, which can then drift out of sync with the accounts the
  transaction actually touches.
- **A label on the transaction rather than the posting.** Rejected: splits (ADR-0012) put several
  Expense legs under one transaction, each wanting its own category. A transaction-level label would
  have to be re-cut the moment splits land.
- **Mandatory labels.** Rejected: it collapses into optional anyway. Some spend genuinely has no
  category yet, so you either lie or invent an "Uncategorized" label to pick — at which point
  mandatory-with-an-escape-hatch and optional-with-a-default are the same design. Better that absence
  *means* Uncategorized and the summary shows it openly.

## Consequences

- Categorization can be reshaped freely — retag, rename, reparent, archive — without touching ledger
  correctness. Net worth, per-account spending, income and every balance never consult labels.
- Splits inherit this for free: several labelled legs under one transaction need no new shape, because
  the assignment already keys on a posting rather than a transaction.
- Promoting categorization into its own bounded context later stays cheap — it already references
  postings by id instead of reaching into the aggregate.
- A posting's **identity** becomes part of the domain, not merely of the database, so a label can be
  bound to the correct leg at the moment the transaction is recorded rather than by re-reading it
  afterwards. This is identity, not metadata; the core still knows nothing of labels.
- Reporting gains a label breakdown alongside the account one (ADR-0013's anticipated "by tag"
  slice). Both must reconcile to the same per-currency total — the check that catches a roll-up bug.
