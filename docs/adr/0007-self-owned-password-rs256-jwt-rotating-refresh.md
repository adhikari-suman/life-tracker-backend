# Authentication is self-owned: password login issuing RS256 JWT access tokens and rotating, Session-backed refresh tokens

We authenticate Users ourselves rather than delegating to a social identity provider. Login
verifies a password (Argon2id, behind a `PasswordHasher` port so no crypto library reaches the
domain) and issues two things: a short-lived **JWT access token** (~15 min), signed **RS256** with
a 3072-bit key and validated by signature on every request exactly as an OAuth2 *resource server*
would; and a long-lived **opaque refresh token**, stored only as a hash and rotated on every use.
Each login lineage is a **Session** — one per device — which reuse-detection revokes wholesale if
a retired refresh token is ever replayed. `User` is a domain aggregate (`domain/user/`) with
`Email` and `PasswordHash` value objects; password hashing and token signing live behind ports,
implemented in infrastructure.

## Why this shape

The goal is to build and understand a finance system, auth included, so we own the primitives
rather than outsourcing them. Two choices are deliberate and forward-looking:

- **Resource-server shape + asymmetric (RS256) signing.** Validating our own tokens — fetch the
  issuer's JWKS, verify the signature — is identical to how we would later validate Google's or
  Apple's. Adding social OIDC becomes *additive* (a second trusted issuer), not a rewrite. HS256
  would have been simpler but would have made our own validation path diverge from the OIDC one.
- **Rotation with reuse-detection over a Session.** A stateless JWT cannot be revoked; a stateful
  Session behind the refresh token can. Storing the refresh token hashed, single-use, and tied to a
  revocable Session keeps "sign out everywhere" and theft-response a one-row flip, with no JWT
  denylist.

## Considered options

- **Delegate to social OIDC / a managed IdP (Auth0, Keycloak, Cognito).** Deferred, not rejected:
  it hides the primitives we want to learn and adds a dependency. The resource-server shape keeps
  the door open to adopt it later.
- **Stateful server sessions for the whole auth path.** Rejected as the primary model: simpler and
  trivially revocable, but not the resource-server shape OIDC expects, so it would force a parallel
  token path later. We keep server state only for the *refresh* side (the Session), never for
  per-request access checks — those stay stateless signature validation.
- **HS256 symmetric signing.** Rejected: secure for a single service, but it diverges from the
  OIDC validation path that motivated JWTs in the first place.

## Consequences

- We own password storage and its flows. Email verification and email-based reset are deferred (no
  SMTP in v1); an `email_verified` seam is left. A forgotten password is a manual reset until then.
- Token lifetimes: access ~15 min; refresh / Session 30 days sliding, under a 90-day absolute cap.
- The private signing key lives in a secret store (never the repo); the public key is served at a
  JWKS endpoint with a `kid`, so keys can rotate.
- Login needs brute-force protection (rate-limit plus temporary lockout) and non-enumerating error
  messages — an implementation follow-on, not re-litigated here.
- Authentication endpoints (register, login, refresh, logout) are part of the wire and belong in
  `life-tracker-contracts/openapi.yaml`, generated — never hand-written in a frontend.
