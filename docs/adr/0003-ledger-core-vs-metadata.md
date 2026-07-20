# The ledger core records only money movement between accounts; everything descriptive is metadata

The double-entry core does exactly one job: **money received and money gone**, as balanced
postings between accounts. An **account** exists only where you hold a running balance you
could reconcile against reality — your bank (against its statement), your credit card (against
its bill), a friend you lent to (a Receivable, against what they actually owe). A counterparty
you settle with on the spot — Walmart, a restaurant, an employer — is **never** an account.
Neither are its branches, nor the line-items on a receipt. All of that — merchant, branch,
item, price, product type, the precise timestamp — is **metadata** attached to a transaction,
shaped and refined separately from the ledger.

The dividing line is sharp: **if it changes a balance, it is in the ledger; if it only
describes the event, it is metadata.** A friend's IOU changes a balance, so it is a Receivable
account. Walmart changes nothing you hold, so it is a metadata reference.

## Considered options

- **Make each merchant or branch an account** (book `DR Walmart:ElmSt`). Rejected: it
  re-explodes the deliberately-coarse account space into thousands of accounts, still needs
  labels to categorize (Walmart sells many categories), and manufactures "accounts" that can
  never be reconciled — a spend total wearing an account's costume.
- **Make each receipt line-item a posting.** Rejected: it bloats the ledger with grocery
  lines, collides with the one-label-per-posting rule (an item has both a spend-category and a
  product-type), and the double-entry genuinely does not care that the £50 was milk and bread.

## Consequences

- There is a single test for "should this be an account?": *only if a balance stands between
  you and it.* This is the answer to the question a future reader will ask — "why isn't Walmart
  an account?"
- Metadata (merchant, branch, items, product types, timestamps) can be added and reshaped
  without touching the ledger's correctness, and is deliberately deferred.
- Reporting by merchant, branch, or item is a query over metadata, not a walk of the account
  tree.
