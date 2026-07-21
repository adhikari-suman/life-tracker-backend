# Sharing a Book: one revocable anonymous Share Link plus named View Grants, read-only, resolved on the read side

An owner may open their whole Book to others in two ways, both read-only and both revocable. A
**Share Link** is a single, anonymous, unguessable URL — "anyone with the link can view" — at most
one active per Book; revoking it **burns that token permanently**, and re-sharing mints a new one,
so a revoked URL can never be reactivated. A **View Grant** names a specific person: the owner
grants read access to an email that *already* belongs to a registered User, who then signs in as
themselves to view. Both grant the *whole* Book (ADR-0005) and touch no write path — viewers live
entirely on the read side.

## Considered options

- **Many Share Links per Book.** Rejected for v1: one "anyone with the link" switch matches the
  Google Drive model this design is patterned on and is simpler. Multiple independently-revocable
  links can come later.
- **How the Share Link token is stored.** Stored **retrievable (as-is)** so the owner can re-copy
  the live link — Google Drive's actual model: a re-copyable link governed by a revocable permission
  and an unguessable capability, not a hashed secret. Security rests on the token's unguessability,
  immediate revocation, and database-level at-rest encryption — deliberately *not* an app-level
  per-link cipher, which is more than Drive itself does. Show-once/hashed storage was rejected
  because the owner could not re-copy the same link; app-level encryption-at-rest is noted as an
  optional hardening. Revocation still **burns** the token (a leaked link can never be reactivated)
  — a deliberate tightening over Drive's reuse-on-re-enable.
- **Share Link expiry by default.** Rejected: revocation is the control; optional expiry is a later
  refinement.
- **Pending invites for View Grants** (granting an email with no account yet). Deferred: v1 grants
  only to existing Users and refuses unknown emails, because email-dependent flows are out of v1.
  Pending-by-email lands next to email verification.

## Consequences

- Read access to any Book resolves to "the owner, OR a User holding a View Grant, OR the holder of
  the active Share Link." This is decided in the query services (ADR-0006) — the one place
  isolation and sharing meet — and is never a write concern.
- Revocation is immediate and permanent for both mechanisms; the Share Link token and each View
  Grant are independently revocable records.
- Because viewer access is read-only and whole-Book, no partial-visibility filtering exists yet;
  slice-level sharing stays the deferred refinement from ADR-0005.
