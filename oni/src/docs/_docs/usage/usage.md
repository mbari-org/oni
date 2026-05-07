# Usage

Oni requires an existing [SQL Server](https://www.microsoft.com/en-us/sql-server/sql-server-downloads) or [PostgreSQL](https://www.postgresql.org) database. The easiest way to spin Oni up with a database is to use [m3-quickstart](https://github.com/mbari-org/m3-quickstart), which handles database setup automatically.

## Configuration

Oni is configured entirely through environment variables. The table below lists all available variables.

### HTTP

| Variable                      | Default                   | Description                          |
|-------------------------------|---------------------------|--------------------------------------|
| `HTTP_PORT`                   | `8080`                    | TCP port the server listens on       |
| `HTTP_CONTEXT_PATH`           | `v1`                      | API context path prefix              |
| `HTTP_STOP_TIMEOUT`           | `90000`                   | Graceful shutdown timeout (ms)       |
| `HTTP_CONNECTOR_IDLE_TIMEOUT` | `90000`                   | Idle connection timeout (ms)         |

### Authentication (JWT)

| Variable                  | Default                    | Description                        |
|---------------------------|----------------------------|------------------------------------|
| `BASICJWT_CLIENT_SECRET`  | *(insecure default)*       | API key used to obtain a JWT       |
| `BASICJWT_SIGNING_SECRET` | *(insecure default)*       | Secret used to sign JWT tokens     |
| `BASICJWT_ISSUER`         | `http://www.mbari.org`     | JWT issuer claim                   |

**Always override `BASICJWT_CLIENT_SECRET` and `BASICJWT_SIGNING_SECRET` in production.**

### Database

| Variable              | Default  | Description                                        |
|-----------------------|----------|----------------------------------------------------|
| `DATABASE_DRIVER`     | *(required)* | JDBC driver class (see examples below)         |
| `DATABASE_URL`        | *(required)* | JDBC connection URL                            |
| `DATABASE_USER`       | `sa`     | Database username                                  |
| `DATABASE_PASSWORD`   | `""`     | Database password                                  |
| `DATABASE_THREADS`    | `16`     | Database thread pool size                          |
| `DATABASE_LOG_LEVEL`  | `INFO`   | Hibernate SQL log level                            |

### Logging

| Variable         | Default | Description                       |
|------------------|---------|-----------------------------------|
| `LOGBACK_LEVEL`  | `INFO`  | Root log level for Logback        |

## Run using Docker

### PostgreSQL

The schema for PostgreSQL is in [02_m3_kb.sql](https://github.com/mbari-org/oni/blob/main/it-postgres/src/test/resources/sql/02_m3_kb.sql).

```sh
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="change-me" \
    -e BASICJWT_SIGNING_SECRET="change-me-too" \
    -e DATABASE_DRIVER="org.postgresql.Driver" \
    -e DATABASE_PASSWORD="xxxx" \
    -e DATABASE_URL="jdbc:postgresql://your.host:5432/YourDatabase?sslmode=disable&stringType=unspecified" \
    -e DATABASE_USER="dbuser" \
    --name=oni \
    --restart unless-stopped \
    mbari/oni
```

### SQL Server

The schema for SQL Server is in [init_min.sql](https://github.com/mbari-org/oni/blob/main/it-sqlserver/src/test/resources/sql/init_min.sql).

```sh
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="change-me" \
    -e BASICJWT_SIGNING_SECRET="change-me-too" \
    -e DATABASE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver" \
    -e DATABASE_PASSWORD="xxxx" \
    -e DATABASE_URL="jdbc:sqlserver://your.host:1433;databaseName=YourDatabase" \
    -e DATABASE_USER="dbuser" \
    --name=oni \
    --restart unless-stopped \
    mbari/oni
```

## Verifying the Service

### Health check

```sh
curl http://localhost:8080/v1/health
```

### Interactive API documentation

Browse to `http://localhost:8080/v1/docs` for the Swagger UI.

OpenAPI specs are also available as:
- YAML: `http://localhost:8080/v1/docs.yml`
- JSON: `http://localhost:8080/v1/docs.json`

### Metrics

Prometheus metrics are exposed at `http://localhost:8080/v1/metrics`.

## Obtaining a JWT

Before calling write endpoints you need a JWT. Use your `BASICJWT_CLIENT_SECRET` to exchange for a token:

```sh
curl -X POST http://localhost:8080/v1/auth \
     -H "Authorization: APIKEY <your-client-secret>"
```

Or log in with a username/password:

```sh
curl -X POST http://localhost:8080/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"username":"admin","password":"password"}'
```

Both return a JSON body containing a `token` field. Pass the token on subsequent requests:

```sh
curl -H "Authorization: Bearer <token>" \
     http://localhost:8080/v1/concept
```
