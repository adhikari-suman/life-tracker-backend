# The database schema is code-first: entities define it, Liquibase versions it, Hibernate validates it

The JPA entity is the source of truth for its table's shape. We write the entity, then a Liquibase
changeset written to match it. Liquibase still *drives* migrations — it applies them, ordered,
versioned and reversible, as a job that runs before the app — and Hibernate runs with
`ddl-auto: validate`, so a drift between an entity and the schema fails the Testcontainers boot
rather than diverging silently. The domain is untouched by this: code-first governs the
entity-to-table relationship only; the pure domain `User` is still mapped to `UserEntity` by hand.

## Why

The developer writes the shape once, on the entity, and the schema follows — the "code-first"
workflow — *without* giving Hibernate authority to write DDL. `validate` buys back the safety of a
generated schema (entity and table must agree) while Liquibase keeps the DDL explicit, reviewable
and reversible, and keeps room for what Hibernate cannot express: grants, roles, exact money
precision, partial indexes.

## Considered options

- **`ddl-auto: update` / `create` — let Hibernate write the schema at runtime.** Rejected: unsafe
  past dev. It never renames or drops, never migrates data, applies changes in a non-deterministic
  order, and is warned against in production. `validate` is the only `ddl-auto` we ever use.
- **Schema-first — hand-author the migration, entity mirrors it.** The original stance (the
  pre-inversion `infrastructure/CLAUDE.md`). We flipped the authority so the entity is the design
  surface, which is the preferred workflow here. Mechanically the two are near-identical under
  `validate`; the difference is which artifact you edit first.
- **Auto-generated migrations via the `liquibase-hibernate` diff.** Tried and rejected. The
  `liquibase-hibernate7:5.0.3` extension *does* run against Hibernate 7.4.1, but its snapshot emits
  `char(36)` for a `UUID` primary key — a long-standing liquibase-hibernate limitation
  ([issue #705](https://github.com/liquibase/liquibase-hibernate/issues/705)), present on Hibernate
  6 and 7 alike, and *not* overridable via `columnDefinition` on the `@Id` (it is honoured for
  other columns, e.g. `timestamptz`, but the identifier column re-derives its type from the Java
  type). An applied `char(36)` id then fails `ddl-auto: validate` against the native `uuid` the app
  maps to, so a generated changeset is self-inconsistent with the runtime. Making it work would
  need a per-run `char(36)`→`uuid` normalization step — not worth it. Each changeset is instead
  hand-written to match the entity and proven consistent at boot. (Downgrading to Spring Boot 3.x
  would not help: same `char(36)` limitation, at the cost of Hibernate 6, Jackson 2, and likely
  Java 21.)

## Consequences

- Every persistence change starts on the entity and is mirrored into a new changeset; the
  Testcontainers boot proves they agree.
- Roles and grants are not expressible on the entity, so they live in the changeset — deferred for
  now, because the `lifetracker_app` role is not yet provisioned and the Testcontainers superuser
  needs no grant.
