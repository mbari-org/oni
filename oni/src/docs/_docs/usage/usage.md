# Usage

Oni requires an existing [SQL Server](https://www.microsoft.com/en-us/sql-server/sql-server-downloads) or [PostgreSQL](https://www.postgresql.org) database that it can connect to. The easiest way to spin Oni up with a database is to use the [m3-quickstart](https://github.com/mbari-org/m3-quickstart) which handles the setup of the database for you. 

## Run using [Docker](https://www.docker.com)

If you have an existing database, you can start Oni using Docker.  Note these examples use place holder values for the secrets, server name and database name that you can subtitues with your own.

### PostgreSQL

Here's an example connecting to a PostgreSQL database. You can find the schema to use in [02_m3_kb.sql](https://github.com/mbari-org/oni/blob/main/it-postgres/src/test/resources/sql/02_m3_kb.sql).

```sh
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="xxxx" \
    -e BASICJWT_SIGNING_SECRET="xxxx" \
    -e DATABASE_DRIVER="org.postgresql.Driver" \
    -e DATABASE_PASSWORD="xxxx" \
    -e DATABASE_URL="jdbc:postgresql://your.serverurl.org:5432/YourDatabaseName?sslmode=disable&stringType=unspecified" \
    -e DATABASE_USER=dbuser \
    --name=oni \
    --restart unless-stopped \
    mbari/oni
```

### SQL Server

The schema for SQL Server is in [init_min.sql](https://github.com/mbari-org/oni/blob/main/it-postgres/src/test/resources/sql/02_m3_kb.sql).

```sh
docker run -d \
    -p 8080:8080 \
    -e BASICJWT_CLIENT_SECRET="xxxx" \
    -e BASICJWT_SIGNING_SECRET="xxxx" \
    -e DATABASE_DRIVER="com.microsoft.sqlserver.jdbc.SQLServerDriver" \
    -e DATABASE_PASSWORD="xxxx" \
    -e DATABASE_URL="jdbc:sqlserver://database.mbari.org:1433;databaseName=YourDatabaseName" \
    -e DATABASE_USER=dbuser \
    --name=oni \
    --restart unless-stopped \
    mbari/oni
```

## Documentation

Once the server is started you can view the openapi documentation at http://yourservername:8080/docs