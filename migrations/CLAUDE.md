# migrations

Liquibase changelogs. No Java. No Spring. This module builds a Docker image that runs to
completion before the app starts, and does nothing else.

## Rules

- Changesets are APPEND-ONLY once merged. NEVER edit a changeset that has run anywhere —
  Liquibase checksums each one and will refuse to start against a modified changelog. Add a
  new changeset instead.
- One changeset per logical change. Explicit `id` and `author` on every one. Never
  `author: claude`.
- Money columns are `NUMERIC(19,4)`. Never `float`, `real`, `double precision`, and never
  Postgres's `money` type — it is locale-dependent and rounds to the locale's fraction
  digits.
- Every changeset gets a `rollback` block. Liquibase infers some (`createTable`); write it
  explicitly where it cannot (`sql`, `dropColumn`).
- No `ddl-auto` anywhere, ever. Hibernate validates; it never writes.
- Grants for a new table go in the SAME changeset that creates it. A table the app cannot
  read is a production incident, not a test failure.

## Layout

    src/main/resources/db/changelog/
      db.changelog-master.yaml          includes, in order. Nothing else.
      changes/001-create-purchase.yaml
      changes/002-....yaml

Master is an index. Never put a changeset in it.

## Roles

- `lifetracker_migrator` — owns the schema, has DDL. Only the migration job uses it.
- `lifetracker_app` — DML only. `REVOKE CREATE ON SCHEMA public`. The application cannot
  ALTER a table even if Hibernate wanted to.

## The drift check is the point

`ddl-auto: validate` plus Testcontainers means: add a field to an entity and forget the
changelog, and Spring refuses to boot with "missing column" during the integration test. If
you see that error, the changelog is missing. Write it. Do not touch `ddl-auto`.