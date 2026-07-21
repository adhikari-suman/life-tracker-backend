# Login brute-force is bounded per email by a configurable sliding-window throttle

Login tolerates at most `app.auth.login.max-attempts` failed attempts for a given email within a
trailing `app.auth.login.window` (both config; defaults 10 in 1h). The counter is keyed on the
**submitted email** — existent or not — and a miss is counted exactly like a wrong password. Once
the window holds enough failures, `Authenticate` raises `TooManyAttemptsException` *before* it looks
at the password, and the boundary answers `429` with a `Retry-After` header and the code
`TOO_MANY_ATTEMPTS`. A successful login clears that email's failures; an attempt already rejected by
the throttle is not itself recorded. This closes the follow-on flagged in ADR-0007.

## Why this shape

- **Keyed by email, counting misses too.** ADR-0007 made every login failure indistinguishable so
  login never reveals which emails exist. A per-email lockout that fired only for *real* accounts
  would undo that — a `429` would confirm the email is registered. Counting attempts against the
  submitted string whether or not a User owns it keeps a lockout as mute as a wrong password.
- **Sliding window, not a fixed bucket.** "At most x in the trailing y" is what a person means by
  brute-force protection; a fixed window that resets on the hour lets an attacker fire 2x across the
  boundary. We store timestamped failures and count those newer than `now − window`.
- **Lockout bounded to one window past the last real failure.** A throttled attempt is not recorded,
  so an attacker who keeps knocking cannot hold the account locked forever; the lockout drains a
  window after the last *counted* failure, and `Retry-After` is computed from exactly that moment.
- **State in Postgres, like everything else.** One `login_attempts` table, indexed by
  `(email, attempted_at)`. No Redis, no in-memory map that dies with the process or splits across
  instances. Recording a failure prunes that email's rows older than the window, so the table stays
  bounded.

## Considered options

- **Per-(email + IP), or a second per-IP counter.** Stronger — it removes the one real weakness
  below and also catches a single IP sweeping many accounts. Deferred as the drop-in follow-on: it
  needs the client IP threaded to the use case, with trustworthy `X-Forwarded-For` handling behind
  the proxy — more moving parts than the first cut warrants. Neither the port nor the policy changes
  shape when it lands; only the key does.
- **Hard lockout, exponential backoff, or CAPTCHA.** A sliding count is the simplest thing that
  bounds guessing; backoff and CAPTCHA are refinements that layer on without redesigning this.
- **In-memory or Redis counters.** Rejected: in-memory survives neither a restart nor a second
  instance; Redis is infrastructure we do not otherwise need yet.

## Consequences

- **A known, accepted weakness: victim lockout.** Someone who knows your email can lock *you* out
  for up to one window by failing on purpose. They still cannot get in, and it self-heals when the
  window passes. The per-IP upgrade above removes it; until then it is the price of the simplest
  design that does not leak existence.
- The `429`, its `TOO_MANY_ATTEMPTS` code, and the `Retry-After` header are part of the wire —
  `life-tracker-contracts/openapi.yaml`, generated, never hand-written in a frontend.
- The timing-oracle noted in ADR-0007 (skipping the hash on an unknown email) is still open and
  independent of this change.
- `Authenticate` now depends on a `LoginAttempts` port, a `LoginThrottle` policy, and a `Clock`; the
  policy's limit and window come from config, so tuning is a redeploy, not a code change.
