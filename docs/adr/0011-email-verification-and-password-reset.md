# Email verification and password reset ride opaque one-time tokens; delivery is a stub port, and sharing is gated on verification

Registration now issues an **email-verification** token, and the flows a User drives from an
unverified or forgotten email are real: verify, resend, request-reset, confirm-reset. Each rides an
**opaque, single-use, expiring one-time token** — 32 bytes of `SecureRandom`, stored only as its
SHA-256 hash, exactly like a refresh token (ADR-0007) — carrying a `purpose` (`VERIFY_EMAIL` ~24h /
`RESET_PASSWORD` ~1h). Delivery goes through an **`EmailSender` port** whose only implementation today
logs the token; real SMTP is a later adapter swap, so ADR-0007's "no SMTP in v1" still holds.
Verifying the email is what unlocks **sharing**: an unverified owner gets `403 EMAIL_NOT_VERIFIED`
from mint-Share-Link and grant-View. This closes the verification/reset follow-on ADR-0007 deferred.

## Why this shape

- **One token concept, two purposes.** Verification and reset are the same primitive — prove control
  of an email via a link only its holder receives. A single `OneTimeToken` aggregate with a
  `TokenPurpose` avoids two near-identical tables and hashers; the use cases differ, the token does
  not. Hashed-at-rest and single-use mean a leaked database row cannot be replayed into a live link.
- **Delivery is a port, not the feature deferred again.** ADR-0007 deferred these *because* there is
  no email transport. Rather than defer the whole thing once more, the flows, tokens, storage, and
  endpoints are built for real and only the transport is stubbed — a logging `EmailSender`. Swapping
  in SES/SMTP later touches one class, behind config, and nothing else moves.
- **Verification gates sharing, nothing else yet.** ADR-0007 fixed that verification gates *what you
  may do*, never *whether you are signed in*. Sharing is the one outbound surface that exists, and
  handing out "come read my finances" links or grants from an unverified, possibly-fake identity is
  exactly what should wait for proof of the address. Writing your own Book stays open; the ledger's
  own gate arrives with the ledger.
- **Reset is non-enumerating and revokes sessions.** `POST /auth/password-reset` answers `202`
  whether or not the email exists — consistent with the login work (ADR-0010), so it never confirms
  who has an account. A completed reset revokes every Session: a reset answers a possible takeover,
  so existing logins must die with the old password.

## Considered options

- **Wire real SMTP / a provider now.** Rejected for v1: it drags in credentials, deliverability, and
  a dependency for a learning-stage backend, and contradicts ADR-0007's deferral. The port makes it
  a drop-in when it is wanted.
- **Two separate token types / tables.** Rejected as duplication: identical shape and lifecycle,
  distinguished by one `purpose` column.
- **Gate nothing until the ledger.** Rejected: it ships verification as a dormant flag with no
  observable effect and no test, easy to forget to wire. Gating sharing gives it teeth today.
- **Put verification state in the access-token claims** to gate without a DB read. Rejected for now:
  a 15-minute access token would carry stale verification state after a user verifies mid-session.
  The use case reads the User, so the check is always fresh.

## Consequences

- A new `one_time_tokens` table (changeset 006); a stub `EmailSender` (logging) swapped for a
  capturing double in tests to complete the round-trip. Issuing a token for a purpose invalidates the
  User's prior tokens of that purpose, so only the latest link works.
- `/me` now returns `emailVerified`, so a client can prompt for verification and know sharing is
  gated. The new endpoints (`verify-email`, `verify-email/resend`, `password-reset`,
  `password-reset/confirm`) live in `life-tracker-contracts`, generated, never hand-written.
- Sharing use cases now load the owner to check verification; the ADR-0006 tenant read guard is
  unaffected.
- Still open: the real SMTP adapter, re-verification when a User changes their email, and a sweep of
  expired token rows (hygiene only — a consumed or expired token cannot be used, like the
  login-attempt prune in ADR-0010).
