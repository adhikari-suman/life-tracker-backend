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
- **Auto-generated migrations via the `liquibase-hibernate` diff.** Deferred, not rejected: on
  brand-new Spring Boot 4.1 / Hibernate 7 the extension's compatibility is unverified, and a
  generated changeset still needs hand-finishing for grants, rollback and author. For now each
  changeset is hand-written to match the entity; the diff tooling can be added when the payoff
  grows.

## Consequences

- Every persistence change starts on the entity and is mirrored into a new changeset; the
  Testcontainers boot proves they agree.
- Roles and grants are not expressible on the entity, so they live in the changeset — deferred for
  now, because the `lifetracker_app` role is not yet provisioned and the Testcontainers superuser
  needs no grant.
