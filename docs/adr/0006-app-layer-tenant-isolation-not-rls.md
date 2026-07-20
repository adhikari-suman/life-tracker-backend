# Tenant isolation lives in the application, not the database; a mandatory OwnerId plus an ArchUnit rule keeps it structural

Every tenant-owned row carries an `owner_id`, and the rule "you only ever touch your own Book" is
enforced in the **backend**, not by Postgres Row-Level Security. Each use case and query service
takes an `OwnerId` — threaded from the authenticated token, never trusted from the request body or
query string — and scopes every read and write by it. An **ArchUnit rule fails the build** if any
use case or query read lacks that parameter, and a cross-tenant test harness (User B must never see
User A's data) backs it in CI.

## Why this is written down

A reader steeped in ADR-0001 will expect the *structural* choice here — RLS, where a forgotten
`WHERE` cannot leak because the database itself refuses. We deliberately chose the application
layer instead, for two reasons: we want the isolation logic explicit and readable in Java rather
than hidden in database policies, and we want to stay database-agnostic. The obvious risk of the
app layer is ADR-0001's own nightmare — a query added later with no owner filter and no test, which
leaks silently. The **ArchUnit guard is what buys back the "structural, not remembered" property**:
a missing filter becomes a compile/CI failure, not a latent breach. That combination — explicit
Java enforcement made non-forgettable by an architecture test — is the decision.

## Considered options

- **Postgres Row-Level Security.** Rejected despite fitting ADR-0001's philosophy best: it ties
  isolation to Postgres and pushes the rule below the code, where it is harder to read and test.
- **Discipline plus tests alone.** Rejected as insufficient: tests only cover the leaks you thought
  of; the dangerous query is the untested one. Tests are kept as defense-in-depth, not the primary
  guarantee.

## Consequences

- The acting `OwnerId` is an explicit input to every use case and query service, sourced from the
  token.
- The cross-owner invariant (a transaction may not post across two owners' accounts) holds for
  free: owner-scoped repositories can only load your own accounts, so a cross-owner posting cannot
  be constructed — the domain aggregate needs no owner field.
- Read access for viewers (own Book *or* a Book you hold a grant / valid link for) is resolved in
  the query services, against the grant and Share Link records — the one place isolation and
  sharing meet.
- If isolation coverage ever feels fragile, the ArchUnit rule is the first thing to strengthen.
