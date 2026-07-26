# smoke

Two suites that run against a **running stack**, not against mocks.

```sh
docker compose up -d --build --scale app=1
smoke/run.sh
```

No dependencies beyond `docker`, `python3` and `bash` — deliberately, so this stays runnable
from a fresh checkout without a package install.

| | What it checks |
|---|---|
| [`api.py`](./api.py) | The whole HTTP surface against `life-tracker-contracts/openapi.yaml` and the domain rules in [`CONTEXT.md`](../CONTEXT.md) and [`docs/adr/`](../docs/adr) |
| [`database.sh`](./database.sh) | The two-role split, grants, column types, and the ledger invariants as they sit in stored rows |

## Why these exist alongside `./gradlew test`

They are not a replacement, and they do not duplicate it. The Gradle suite owns the units and the
persistence, and it is where new behaviour should be pinned first. What it structurally cannot do
is compare the assembled application to the **spec**, because nothing in Java reads
`openapi.yaml`.

Both of these were found by `api.py` while the entire Gradle suite was green:

- `time` came back as `"19:42:00"`, violating the spec's own
  `pattern: ^([01][0-9]|2[0-3]):[0-5][0-9]$`. A generated client validating responses would have
  rejected it.
- Omitting `time` threw `NullPointerException` into a **500** rather than a 422, because the DTO
  carried no `@NotNull`.

Each is now also pinned in `LedgerEndpointsIntegrationTest` — that is the right long-term home.
These suites are the net that catches the *next* one.

Likewise `database.sh` covers things no HTTP call can observe: that `lifetracker_app` is genuinely
unable to `CREATE`, `ALTER` or `DROP`, that every table carries all four DML grants (checked
exhaustively — one table missing one verb is exactly what `ALTER DEFAULT PRIVILEGES` exists to
prevent, and it stays invisible until that code path runs), and that money is `NUMERIC(19,4)`
rather than anything that rounds.

## Reading a failure

Every check names the **rule** it defends, not just the endpoint it calls, so a failure should
tell you which decision broke:

```
  ok   spending excludes transfers/payments/loans by construction
  ok   summing byLabel.own reconciles to totals (the roll-up check)
  FAIL a missing time is refused (422, not 500)
         expected: 422
         actual:   500
```

## Notes

- **`api.py` is safe to re-run.** It registers a fresh user per run and asserts only within that
  Book, so its totals are deterministic without needing an empty database.
- **It writes real data.** The ledger is append-only, so its transactions cannot be deleted
  afterwards — point it at a development stack, never at anything you care about.
- **`database.sh` reads the whole database**, including rows other runs left behind, so its
  integrity checks get broader over time rather than narrower.
- Email verification is exercised for real by reading the token `LoggingEmailSender` prints to
  the container log — which is also why `APP_CONTAINER` is configurable.

| Variable | Default |
|---|---|
| `API_BASE_URL` | `http://localhost:8080/v1` |
| `APP_CONTAINER` | `life-tracker-backend-app-1` |
| `DB_CONTAINER` | `life-tracker-backend-db-1` |
| `DB_NAME` | `lifetracker` |
