# The ledger is multi-tenant: one User identity, owners write and viewers read, one Book per User

Life Tracker hosts many independent people. Each is a **User** — the single kind of identity;
everyone who signs in is one. A User owns exactly one **Book** — the whole of their accounts,
transactions, and labels — and only the owner may write it. Other Users may be granted *read-only*
access, either as an **authenticated viewer** (a named User with a View Grant) or an **anonymous
viewer** (whoever holds a Share Link). "Owner" and "viewer" are relationships to a Book, not kinds
of User, so the same identity that views your Book can own their own. This is the Google Drive
model: one account, roles that are contextual per document.

## Considered options

- **Single-tenant (one owner, many viewers).** Rejected: the product is meant to host many
  people's books, each writing their own; a single implicit owner cannot express that.
- **Separate "owner" and "viewer" account types.** Rejected: it blocks a viewer from ever starting
  their own Book without a second signup, and splits authentication into two flavours for no gain.
  One unified User makes authorization a single question — *what is this User's relationship to the
  Book being touched?*
- **Many Books per User.** Deferred, not rejected: one Book per User keeps the tenant key the
  User's own id and needs no separate Book table. Going 1:N later is additive — introduce a Book id
  and fold each User's rows into one default Book.
- **Slice-level sharing (share an account, a label, a date range).** Deferred: the headline
  numbers (net worth, spending) are only coherent over a whole Book, so v1 shares the whole Book or
  nothing. A scope can be added to a grant later.

## Consequences

- The domain gains no owner field on its aggregates; ownership is enforced around the Ledger, not
  inside it (see ADR-0006), so the Ledger core (ADR-0003) stays about money movement only.
- Viewers live entirely on the read side — query services, never use cases — because they never
  write.
- The word "Account" stays a Ledger term; a user account is a User. The vocabulary split is
  recorded in `CONTEXT-MAP.md`.
