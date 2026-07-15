# infrastructure

Everything that touches the outside world. The Spring Boot main class lives here.

    infrastructure/persistence/   JPA entities, mappers, adapters, query services
    infrastructure/web/           controllers, request/response DTOs
    infrastructure/config/        Spring configuration

## Persistence

- `PurchaseEntity` mirrors the TABLE, not the domain class. Columns, not concepts. When the
  two want to diverge — a `version` column, a soft-delete flag, a denormalized
  `merchant_name` — the entity changes and `Purchase` does not. That divergence is the
  reason the second class exists.
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