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
- NEVER write a `GRANT` in a changeset. `lifetracker_app` gets its DML from `ALTER DEFAULT
  PRIVILEGES`, set once when the database is created, so every table the migrator creates is
  granted automatically and no changeset can forget one (ADR-0016).

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

Both are created by `docker/postgres/init/01-roles.sh`, which also holds the default privileges.
That one script is used in both places: the compose stack mounts it, and the Testcontainers boot
mounts the same file, so the tests prove the script rather than a second copy of it. Liquibase
connects as the migrator and the application as the app role in tests too — a missing privilege
fails the suite instead of production.

## The drift check is the point

`ddl-auto: validate` plus Testcontainers means: add a field to an entity and forget the
changelog, and Spring refuses to boot with "missing column" during the integration test. If
you see that error, the changelog is missing. Write it. Do not touch `ddl-auto`.