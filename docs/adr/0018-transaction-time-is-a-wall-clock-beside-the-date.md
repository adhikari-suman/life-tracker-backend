# A transaction carries a wall-clock time beside its date, not an instant

A transaction records **when the money moved** as a calendar date plus a **zoneless wall-clock
time** — `19:42` means 19:42 where the person was standing. The date remains the sole key every
report groups by; the time orders a day and does nothing else. It is supplied by the client from
the device's clock and is required, so the server never guesses a moment it cannot know.

This supersedes the part of ADR-0003 that filed "the precise timestamp" among the deferred
metadata. The rest of that ADR stands: merchant, branch, items and product types are still
metadata, and the test it gives — *if it changes a balance it is in the ledger, if it only
describes the event it is metadata* — still holds. A time does not change a balance. It is
promoted anyway because it turned out to be needed for something metadata cannot do: put a day in
the order it happened.

## Considered options

- **A single `timestamptz`, a true instant.** Correct in the absolute sense, and the only model
  that survives someone travelling between zones. Rejected because every report would then have to
  derive a calendar date from an instant, which requires picking a timezone — and a 23:30 purchase
  in UTC-5 becomes the 25th in UTC and moves between months. The codebase already refuses this
  trade in `dateRange.ts`: *"A ledger's dates are the dates on the user's receipts, and
  `toISOString()` would shift them across the date line … silently moving a transaction into the
  previous month."* An instant would make that failure a property of storage rather than of one
  function.

- **A zoneless `timestamp` replacing `tx_date`.** Tidier than two columns and equally immune to
  drift. Rejected because it makes `tx_date` — the key every report groups by, and the one column
  whose meaning is settled — collateral in a change that is really about ordering, and it forces
  a time onto all 27 existing rows as part of the same migration rather than as a decision.

- **The server defaulting the time from its own clock.** Rejected outright: the server runs in UTC
  and does not know the user's zone, so its "now" is the wrong wall clock for everyone not sitting
  in the datacentre. The client is the only party that knows what the clock on the wall said.

- **Nullable, with an absent time meaning "not recorded".** This is what Branch does, and what the
  glossary means by *"that absence is a fact about the event, not missing data"*. Rejected in
  favour of one shape everywhere — see the consequence below, which is the real cost of that
  choice and is accepted knowingly.

## Consequences

- **`time` is required on `RecordTransactionRequest`.** A breaking wire change. It follows from
  the column being `NOT NULL` and the server refusing to guess: if neither party supplies a time,
  there is nothing honest to store. `life-tracker-web` is the only client today, which is the
  cheapest this break will ever be.

- **A backdated entry records a time that was never true.** Recording Tuesday's coffee on Friday
  evening stamps Friday's clock reading onto Tuesday, and nothing distinguishes it from a time
  that was meant. The ledger is append-only, so it cannot be corrected — only reversed. This was
  weighed and accepted: the alternative was a nullable column, and one shape everywhere was judged
  worth more than the honesty of an absent value.

  Note what that costs precisely, because it is easy to miss: with the column `NOT NULL` and the
  field required on the wire, **there is no way to say "unknown"**. A client cannot decline to
  answer. So undoing this later is not a UI tweak — it means making the column nullable, making
  the field optional, and deciding what the already-stamped rows meant. That is the whole reason
  this consequence is written down rather than left to be discovered.

- **The 27 transactions predating this get `created_at::time`.** A real moment, but the wrong one:
  it is when they were typed, presented as when they happened. Preferred over midnight, which is
  equally untrue and additionally sorts every old transaction to the head of its day.

- **Ordering becomes `tx_date desc, tx_time desc, created_at desc`.** This changes what the recent
  list means: it now reads in the order things happened rather than the order they were typed, so
  catching up on a week no longer shows Tuesday's dinner above Tuesday's breakfast. `created_at`
  survives only as a tiebreak, which keeps the ordering total and stable — without it, two entries
  in the same minute could swap places between renders.

- **Reporting is untouched.** `ReportQueryService` groups by the transaction's date and never sees
  the time. That is the property the wall-clock model was chosen to protect, and it is worth
  stating so that nobody later "improves" the reports by grouping on a derived timestamp.
