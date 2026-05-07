# Development

## Prerequisites

- JDK 21 or later
- [sbt](https://www.scala-sbt.org) 1.x
- Docker (required for integration tests)

## Project Structure

Oni is a multi-module sbt project:

| Module        | Description                                              |
|---------------|----------------------------------------------------------|
| `oni`         | Main application (server, endpoints, services, entities) |
| `it`          | Database-agnostic integration test helpers               |
| `it-postgres` | Integration tests against PostgreSQL (via Testcontainers)|
| `it-sqlserver`| Integration tests against SQL Server (via Testcontainers)|

Within the `oni` module, source is organized as:

```
oni/src/main/scala/org/mbari/oni/
  endpoints/    # Tapir endpoint definitions (one file per API group)
  services/     # Business logic and caching
  jpa/
    entities/   # Hibernate-mapped JPA entities (Java)
    repositories/ # Repository interfaces
  config/       # AppConfig, JwtConfig, HttpConfig, DatabaseConfig
  Main.scala    # Entry point
```

## Useful SBT Commands

| Command                              | Description                                          |
|--------------------------------------|------------------------------------------------------|
| `compile`                            | Compile all modules                                  |
| `test`                               | Run unit tests                                       |
| `itPostgres/test`                    | Run integration tests against PostgreSQL             |
| `itSqlserver/test`                   | Run integration tests against SQL Server             |
| `itPostgres/testOnly <test-class>`   | Run a single integration test against PostgreSQL     |
| `itSqlserver/testOnly <test-class>`  | Run a single integration test against SQL Server     |
| `stage`                              | Build a runnable staging directory at `target/oni/universal/stage` |
| `Docker/stage`                       | Generate a Dockerfile at `target/oni/docker/stage`   |
| `docker:publishLocal`                | Build a local Docker image                           |
| `doc`                                | Generate Scaladoc + this site to `target/docs/site`  |
| `scalafmtAll`                        | Format all source files (Scala 3 indent-based style) |

## Running Locally

Start a PostgreSQL instance (Docker is easiest):

```sh
docker run -d \
  -e POSTGRES_PASSWORD=password \
  -e POSTGRES_DB=oni \
  -p 5432:5432 \
  postgres:16
```

Then run the application directly from sbt:

```sh
sbt "oni/run"
```

Oni will apply Flyway migrations automatically on first start.

## Adding Documentation

Markdown files placed under `oni/src/docs/_docs/` are automatically picked up by `scaladoc` when you run `doc`. Subdirectories create sections in the generated site.

## Architecture Notes

### Startup Sequence

`Main.scala` performs the following on startup:

1. Forces JVM timezone to UTC (prevents JDBC timestamp conversion issues)
2. Reads configuration from `reference.conf` (overridden by environment variables)
3. Creates the Hibernate `EntityManagerFactory` and runs Flyway migrations
4. Starts the Vert.x HTTP server with gzip compression and a worker thread pool
5. Registers all Tapir endpoint routes, the Swagger UI, and the Prometheus metrics handler
6. Attaches request/response logging (DEBUG for all requests, INFO for response timing)

### Caching

`FastPhylogenyService` builds an in-memory tree of the entire knowledgebase using Caffeine and serves phylogeny queries (up/down/taxa/siblings) entirely from cache. The cache is invalidated on any concept write.

Individual JPA entities are also cached at the Hibernate second-level cache layer via the Caffeine JCache integration configured in `application.conf`.

### Authentication Flow

1. Client calls `POST /v1/auth` with `Authorization: APIKEY <client-secret>` or `POST /v1/auth/login` with credentials
2. Server returns a signed JWT
3. Client includes `Authorization: Bearer <token>` on subsequent write requests
4. Tapir security logic validates the JWT on each protected endpoint

### Error Handling

Services return `Either[Throwable, T]`. Endpoints translate the `Left` cases to appropriate HTTP status codes. The Vert.x error handler distinguishes client errors from server faults and logs only unexpected 5xx errors at ERROR level.

## Testing

Integration tests use [Testcontainers](https://testcontainers.com) to spin up real database containers. No manual database setup is needed — Docker must be running.

Test classes are organized under the `it` module's shared helpers with database-specific suites in `it-postgres` and `it-sqlserver`.
