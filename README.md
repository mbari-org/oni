# oni - Organism Naming Infrastructure

![Build](https://github.com/mbari-org/oni/actions/workflows/test.yml/badge.svg)

![MBARI logo](oni/src/docs/_assets/images/logo-mbari-3b.png)

## Overview

This is a project to create a RESTful API for managing a tree structure of names used by annotation tools and data catalogs. We typically call this tree structure a "knowledgebase" (aka "KB"). Tree structures are useful for modeling organism phylogeny. At MBARI, we also include other terms, such as geological features and equipment used for research. 

Currently in development, it is a replacement for <https://github.com/mbari-org/vars-kb-server> and <https://github.com/mbari-org/vars-user-server>. 

Oni includes APIs for fast search and retrieval of terms, fetching branches of the knowledgebase, and user accounts. Individual nodes in the KB are called _concepts_, and each concept may have one primary name (e.g. the accepted taxa name) and zero or more alternative names (such as synonyms, common names, former taxa names, etc.)

## Usage

Oni requires an existing [SQL Server](https://www.microsoft.com/en-us/sql-server/sql-server-downloads) or [PostgreSQL](https://www.postgresql.org) database that it can connect to. The easiest way to spin Oni up with a database is to use the [m3-quickstart](https://github.com/mbari-org/m3-quickstart) which handles the setup of the database for you. 

### Run using [Docker](https://www.docker.com)

If you have an existing database, you can start Oni using Docker.  Note these examples use place holder values for the secrets, server name and database name that you can subtitues with your own.

#### PostgreSQL

Here's an example connecting to a PostgreSQL database. You can find the schema to use in [02_m3_kb.sql](it-postgres/src/test/resources/sql/02_m3_kb.sql).

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

#### SQL Server

The schema for SQL Server is in [init_min.sql](it-sqlserver/src/test/resources/sql/init_min.sql).

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

Once the server is started you can view the documentation at http://<yourservername>:8080/docs


## Development

This is a normal [sbt](https://www.scala-sbt.org) project. You will need Docker installed to run the full test suite. Docker allows this project to start database servers for integration testing.

## Useful SBT Commands

1. `stage` - Builds a runnable project in `target/oni/universal/stage`
2. `Docker/stage` - Builds a Dockerfile for Oni at `target/oni/docker/stage`
3. `doc` - Build documentation, including API docs to `target/docs/site`
4. `compile` then `scalafmtAll` - Will convert all syntax to new-style, indent based Scala 3.
5. `test` run all tests
6. `itPostgres/test` or `itSqlserver/test` to only run tests against one of the databases.
7. `itPostgres/testOnly <testname>` or `itSqlserver/testOnly <testname>` to run a single test.

