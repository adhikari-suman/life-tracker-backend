# life-tracker-backend

A personal financial ledger, double-entry. This file covers **running it**. For what the words
mean, read [`CONTEXT-MAP.md`](./CONTEXT-MAP.md) and the two glossaries it points at; for why
things are the way they are, [`docs/adr/`](./docs/adr).

Requires a JDK 25 toolchain and Docker. Nothing else — Gradle arrives via the wrapper.

## Running it

Two modes. The first is the one you want almost always.

### App on the host, dependencies in Docker

```sh
docker compose up -d
SPRING_PROFILES_ACTIVE=local ./gradlew :infrastructure:bootRun
```

`docker compose up` brings up Postgres, generates a signing keypair, and applies the migrations —
and stops there. The app is deliberately **not** started, so port 8080 is free for the one you run
from Gradle. Rebuilding the image costs a cold Gradle run per change; `bootRun` costs seconds.

The `local` profile supplies the three datasource values that `application.properties` leaves
undefaulted, and points `app.jwt.*` at the keypair on disk rather than at the container's mount
path. It connects as `lifetracker_app`, the same unprivileged role the container uses.

The API is on `http://localhost:8080/v1`. Postgres is published on 5432, so `psql` works.

### Everything in Docker

```sh
docker compose --profile full up -d --build
```

Adds the app as a container, built from `infrastructure/Dockerfile`. Use it to exercise the real
image — the layered build, the non-root user, the healthcheck — not to iterate on Java.

**Stopping it needs the profile too:** `docker compose down` only touches services in active
profiles, so it leaves a running `app` behind. Use `docker compose --profile full down`.

## What the stack is

| Service | Image | Notes |
|---|---|---|
| `db` | `postgres:18-alpine` | same tag the tests pin; creates both roles on first start |
| `keygen` | `alpine/openssl` | writes a 3072-bit RS256 keypair to `.secrets/` if absent |
| `migrate` | built from `migrations/` | Liquibase 4.33.0; runs to completion before the app |
| `app` | built from `infrastructure/` | profile `full` only; 8080 published, 8081 (health) not |

`.secrets/` is gitignored and per-machine. It is generated once and reused, so restarting does not
invalidate anyone's tokens. Delete it and the next `up` makes a new pair.

`docker compose down -v` drops the database volume, which is also what makes the role-creation
script run again.

## Database roles

`lifetracker_migrator` owns the schema and is the only role with DDL. `lifetracker_app` has
`SELECT, INSERT, UPDATE, DELETE` and cannot create or alter anything. Both are created by
`docker/postgres/init/01-roles.sh`, which the compose stack and the Testcontainers boot mount —
the same file, so the tests prove the script rather than a copy of it.

The app role's grants come from `ALTER DEFAULT PRIVILEGES`, not from a `GRANT` in each changeset.
See [ADR-0016](./docs/adr/0016-grants-from-default-privileges-not-changesets.md); the short version
is that a rule you must remember per changeset is a rule you can forget per changeset.

Migrations can also be driven from Gradle, which defaults to this stack as the migrator:

```sh
./gradlew :migrations:update
```

## Tests

```sh
./gradlew test
```

Testcontainers Postgres, never H2. The suite runs the same two-role split as the stack, so a
missing grant fails here rather than in production — `DatabaseRolesIntegrationTest` asserts that
directly, because every other test would still pass if the app connected as a superuser.

## Two things that will bite you

**The API is served under `/v1`.** The application owns that prefix, not a gateway
([ADR-0017](./docs/adr/0017-the-application-owns-the-v1-prefix.md)). `/auth/login` is a 404;
`/v1/auth/login` is the endpoint.

**Health is on port 8081, not under `/v1`.** It is not part of the versioned API. The port is not
published to the host — reach it from inside the network, or from the container itself.
