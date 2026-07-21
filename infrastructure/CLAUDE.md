# infrastructure

Everything that touches the outside world. The Spring Boot main class lives here.

    infrastructure/persistence/   JPA entities, mappers, adapters, query services
    infrastructure/web/           controllers, request/response DTOs
    infrastructure/config/        Spring configuration

## Persistence

- **The entity defines the table (code-first).** The `@Entity` is the design surface for its
  table: you shape the columns on the entity, and the Liquibase changeset is written to match
  it — the DB follows the code. Liquibase still *drives* migrations (it applies them, versioned
  and reversible, as a job before the app); `ddl-auto` stays `validate`, so if an entity and its
  migration ever drift, the app refuses to boot in the Testcontainers test. See ADR-0009.
- **The entity is still not the domain class.** `UserEntity` holds columns, the domain `User`
  holds concepts, and they diverge on purpose — a `version` column, a soft-delete flag, a
  denormalized `merchant_name` live on the entity and never touch `User`. That divergence is why
  the second class exists. A mapper (plain static methods) converts between them.
- `@Entity` classes are package-private wherever possible. Nothing outside
  `infrastructure/persistence` may reference one.
- `JpaPurchaseRepository implements PurchaseRepository` — the port from `domain`. A Spring
  Data interface is an internal detail of the adapter, never the port itself.
- Mappers are plain classes with static methods. No MapStruct, no reflection. The mapper is
  where money gets reassembled; it must be readable by eye.
- Money: entity holds `total_amount` (BigDecimal) + `total_currency` (String). The mapper
  builds `Money`. NEVER `@Embeddable` — that puts `jakarta.persistence` in the domain.

## Web

- Controllers are thin: parse, call one use case, map the result. No business logic.
- Request/response DTOs are separate from application DTOs. The wire format changing must
  not ripple into a use case.
- Money on the wire is `{ "amount": "12.34", "currency": "USD" }` — the amount is a
  **string**. A JSON number is an IEEE 754 double in JavaScript. This is not stylistic.
- Screen-shaped endpoints belong here. `DashboardController` may call three use cases and
  assemble one response. That is a controller's job.
- Errors: a domain exception maps to a status code HERE, in an `@RestControllerAdvice`.
  The domain does not know what 422 is.

## Reads

Query services live in `persistence/`, return flat DTOs, and may write SQL directly. They do
not load aggregates. Aggregates enforce invariants on write; a dashboard summing 500
purchases has no invariants to enforce, and loading them all is a bug rather than a
shortcut.

## Tests

Testcontainers Postgres. NEVER H2 — it accepts decimal types it does not implement faithfully
and will pass tests that production fails.