# The application owns the `/v1` prefix

`server.servlet.context-path=/v1`, so the backend serves `/v1/auth/login` rather than
`/auth/login`. The version segment in the contract's server URL
(`https://api.lifetracker.example/v1`) is the application's to serve, not a gateway's to add.

## Why

The prefix was nobody's. The controllers map `/auth`, `/accounts`, `/transactions`, `/labels`,
`/reports` with no context path; the contract's `servers` entry ends in `/v1`; and
`life-tracker-web` sets the generated client's base URL to `/v1` and proxies it to the backend
without rewriting the path. Every call from the web client would have 404'd. Nothing caught it
because the backend's tests address controllers directly and the web client had only ever been
driven against stubs.

Given that, someone has to own the segment, and there are only two candidates. A gateway or
ingress is the conventional answer for an OpenAPI `servers` URL carrying a path — but there is no
gateway, locally or anywhere else, and there is no deployment target that would introduce one.
Assigning it to an imaginary component means the alternative fix is a path rewrite in the web
dev-server proxy, stripping a prefix the contract says is part of the base URL: a workaround for
a contract mismatch, living in the consumer, which is precisely the direction this project's
contract rule forbids.

With the app owning it, the local stack matches the contract exactly, the web client needs no
change, and a gateway added later forwards the path untouched instead of having to synthesize it.

## Consequences

- Actuator would move under `/v1` too, which is wrong — health is not part of the versioned API.
  It is instead moved to its own port (`management.server.port=8081`), which also takes it out of
  the `SecurityConfig` filter chain that would otherwise demand a JWT for a healthcheck.
- A future `/v2` is an application-level change. Acceptable: the alternative was an
  infrastructure component that does not exist.
- The contract's `servers` URL is now accurate about who serves what, rather than describing a
  deployment nobody has built.
