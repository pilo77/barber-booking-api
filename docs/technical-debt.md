# Technical Debt

## TECH-DEBT-01: Testcontainers on Windows Docker pipe

`mvn verify` currently builds successfully and runs unit/controller tests, but
the Spring context test using Testcontainers is skipped when Java cannot resolve
the active Docker Desktop pipe.

Observed environment:

- Docker CLI can use the `desktop-linux` context.
- Testcontainers attempts to use a pipe that returns an invalid/empty Docker
  response from Java.

Impact:

- HU-01 and HU-02 are still validated with unit/controller tests.
- Full database-backed integration tests should be enabled before booking logic
  in HU-06, where consistency and overlap rules become critical.

Next action:

- Configure Testcontainers to resolve Docker Desktop's active Windows pipe or
  run integration tests through a stable WSL/Linux Docker context.
