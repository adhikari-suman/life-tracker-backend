# domain

Plain Java. No Spring, no JPA, no Jackson, no Liquibase. `build.gradle.kts` declares no
dependencies and that is the entire point — an import you can't write is better than a rule
you might forget.

## Construction

Objects are valid or they do not exist. Validation goes in the constructor, never in a
`validate()` method someone can forget to call.

- Fields are `final`. Classes are `final` unless there is a reason.
- No no-arg constructors. No setters. No JavaBean anything.
- Throw a named domain exception (`NegativeAmountException`), not `IllegalArgumentException`.
- No `null` fields. If something is genuinely optional, model it explicitly.

## Value objects are records. Aggregates are classes.

    record Money(BigDecimal amount, Currency currency)   // equality by value
    final class Purchase { private final PurchaseId id; } // equality by identity

A record's `equals` compares every component, which is right for `Money` and wrong for
`Purchase` — two purchases with the same amount are not the same purchase. Aggregates
compare on ID only.

Records also cannot have a no-arg constructor, which is exactly why they are correct here
and useless for JPA. That's the two-class rule showing up in the language itself.

## GOTCHA: BigDecimal equality is scale-sensitive

`new BigDecimal("2.0").equals(new BigDecimal("2.00"))` is **false**. `compareTo` returns 0,
but `equals` does not — so a `Money` record's generated `equals` is subtly wrong, and so is
every `HashSet<Money>` and every test assertion built on it.

Normalize scale in the compact constructor so every `Money` with the same value has the same
representation. Do not paper over this by overriding `equals` to use `compareTo` — then
`equals` and `hashCode` disagree and `HashMap` breaks.

## Layout — package by aggregate, not by type

    domain/purchase/    Purchase, PurchaseId, PurchaseRepository
    domain/money/       Money, Currency
    domain/merchant/    Merchant, MerchantId

Not `domain/entities/`, `domain/valueobjects/`, `domain/repositories/`. Those group classes
by what they are instead of what they are about, and every feature change then touches every
package.

## Ports

Repository interfaces live here, beside the aggregate they serve. The implementation lives in
infrastructure. The domain declares the shape it needs; storage adapts to it, never the
reverse.

Ports speak domain types: `PurchaseRepository.save(Purchase)`, never `save(PurchaseEntity)`.
Ports are named for what the domain needs, not for the technology behind them.

## Aggregates

One aggregate per transaction. Aggregates reference each other by ID, never by object
reference — `Purchase` holds a `MerchantId`, not a `Merchant`. If you want to traverse from
one to another, that is a query, and queries do not belong here.

## Tests

Plain JUnit. No Spring, no database, no mocks. If a domain test seems to need a mock, the
design is wrong — say so rather than reaching for Mockito.