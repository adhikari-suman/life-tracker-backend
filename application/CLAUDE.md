# application

Use cases. Depends on `domain` only.

## Shape

One class per use case, named as a command: `RecordPurchase`, not `PurchaseService`.
Anything ending in `-Service` accumulates methods until it is a junk drawer with a
transaction annotation on it. A class named after a verb has exactly one reason to exist.

One public method. Input and output are its own DTOs — not domain objects, not web types.

    RecordPurchase.execute(RecordPurchaseCommand) -> PurchaseId

## Rules

- Depends on ports (interfaces in `domain`), never on adapters.
- Owns the transaction boundary. One use case, one transaction, one aggregate.
- No HTTP concepts. No status codes, no `ResponseEntity`, no request objects, no
  `HttpServletRequest`.
- Orchestrates; does not decide. Business rules belong in the aggregate. If a use case
  contains an `if` about money, that logic is in the wrong module.
- Returns IDs and DTOs, not aggregates. Handing a `Purchase` to a controller invites the
  controller to reach into it.

## Reads do not belong here

Use cases are for writes. A screen that needs data gets a query service in
`infrastructure/persistence/` that returns a flat DTO. Do not add `GetPurchaseById` here and
have it load an aggregate to read one field.

## Tests

In-memory fake adapters (`InMemoryPurchaseRepository`), not Mockito. A fake you can assert
against beats a mock you have to configure, and it stays readable when the use case grows a
second collaborator.