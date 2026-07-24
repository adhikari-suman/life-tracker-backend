# Table grants come from default privileges, not from a GRANT in every changeset

`lifetracker_app` gets its DML from a single `ALTER DEFAULT PRIVILEGES FOR ROLE
lifetracker_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES`, set once
when the database is created. Every table the migrator creates thereafter grants DML to the app
automatically. No changeset contains a `GRANT`, and none ever will.

## Why

The original rule (`migrations/CLAUDE.md`) was "grants for a new table go in the SAME changeset
that creates it," on the reasoning that a table the app cannot read is a production incident
rather than a test failure. That reasoning is right and this decision serves it better: a rule
that must be remembered per changeset can be forgotten per changeset, and default privileges
cannot be.

It also resolves a problem the rule could no longer solve. Changesets 001–011 were all written
before either role existed, every one of them noting the grant as deferred. Changesets are
append-only, so those eleven can never gain the `GRANT` the rule asks for. Adhering to the rule
from changeset 012 onward would have produced a schema where grants exist for later tables and
not earlier ones, which is worse than either extreme.

The same init script creates the roles in both places that run a database: the compose stack and
the Testcontainers boot. The tests connect Liquibase as the migrator and the application as the
app role, so a missing privilege fails the suite. Connecting the tests as a superuser — which is
what they did before — meant no permission check could ever fail, and an entity needing a
privilege the app role lacks would have passed every test and failed in production.

## Considered options

- **A `GRANT` in every changeset, as originally written.** Rejected: unachievable for the eleven
  existing tables, and forgettable for every future one.
- **One catch-up changeset granting across all existing tables, then per-table grants after.**
  Rejected: it keeps the forgettable per-table rule, and adds a permanent changeset that has to
  be kept in step with a role that does not exist outside development yet.
- **No role split — connect as one user everywhere.** The status quo, and what ADR-0009 deferred
  to. Rejected now that a real Postgres exists to run the two roles against; the app being unable
  to `ALTER` is the property worth having, and Testcontainers can never demonstrate it.

## Consequences

- Default privileges bind tables created **after** they are set, **by** the role named. A fresh
  database is correct by construction; an existing one would need a one-off catch-up `GRANT`.
- Grants are invisible in the changelog. A reader looking for them finds none, which is why this
  record exists.
- The rule in `migrations/CLAUDE.md` is retired and replaced by a pointer here.
