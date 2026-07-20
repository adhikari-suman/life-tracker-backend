# Context Map

This repo holds two bounded contexts. The **Ledger** is the double-entry core — money movement
between accounts, and nothing else. **Identity & Sharing** is the perimeter around it — who a
person is, and whose Book they may read or write.

The split is deliberate. The Ledger was designed single-user (see ADR-0003, ADR-0004) and knows
nothing of ownership; multi-tenancy and sharing live entirely in the context *around* it, never
inside it. The layout mirrors that: the Ledger is the primary context and sits at the repo root;
Identity & Sharing is the supporting perimeter and sits under `docs/`.

## Contexts

- [Ledger](./CONTEXT.md) — records money movement as balanced postings between accounts.
- [Identity & Sharing](./docs/identity/CONTEXT.md) — Users, authentication, and the read-only
  sharing of a Book.

## Relationships

- **Identity & Sharing → Ledger.** Every Ledger aggregate belongs to a Book, carried as an
  `owner_id` stamped at the application boundary from the authenticated User. The Ledger never
  references a User and holds no ownership field on its aggregates; isolation is enforced *around*
  the Ledger — a mandatory `OwnerId` on every use case and query, guarded by an ArchUnit rule (see
  ADR-0006) — not inside it. This is what keeps the Ledger core pure (ADR-0003).
- **The word "Account" belongs to the Ledger** — a place a balance lives. A *user account* is a
  **User** in Identity & Sharing. The two contexts never share the word; this is a deliberate
  anti-collision, not an accident of naming.
