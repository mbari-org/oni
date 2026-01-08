# oni - Organism Naming Infrastructure

![Build](https://github.com/mbari-org/oni/actions/workflows/test.yml/badge.svg)

![MBARI logo](oni/src/docs/_assets/images/logo-mbari-3b.png)

## Overview

Oni is a web service that provides a RESTful API for managing a tree structure of names used by annotation tools and data catalogs. We typically call this tree structure a "knowledgebase" (aka "KB"). Tree structures are useful for modeling organism phylogeny. At MBARI, we also include other terms, such as geological features and equipment used for research.

## Key Features

- **RESTful API**: Tapir-based endpoints with auto-generated OpenAPI/Swagger documentation
- **Hierarchical Data Model**: Manage tree structures of concepts with names, metadata, links, and media
- **Dual Database Support**: Compatible with both PostgreSQL and SQL Server
- **Authentication**: JWT-based authentication with token creation and validation
- **Audit Trail**: Complete history tracking of all changes
- **High Performance**:
  - In-memory phylogeny tree caching with Caffeine
  - JPA second-level caching for entities
  - Connection pooling with HikariCP
- **Database Migrations**: Automated schema management with Flyway
- **Modern Stack**: Built with Scala 3, Vert.x, and Hibernate 6
- **Type Safety**: Comprehensive type-safe API definitions and error handling
- **Production Ready**: Containerized deployment with health checks and monitoring

## Docker Deployment

### Quick Start

Pull and run the latest image from Docker Hub:

```bash
docker pull mbari/oni:latest

docker run -d \
  -p 8080:8080 \
  -e BASICJWT_CLIENT_SECRET="your_client_secret" \
  -e BASICJWT_SIGNING_SECRET="your_signing_secret" \
  -e DATABASE_URL="jdbc:postgresql://host.docker.internal:5432/oni" \
  -e DATABASE_USER="your_db_user" \
  -e DATABASE_PASSWORD="your_db_password" \
  --name oni \
  mbari/oni:latest
```

The API will be available at `http://localhost:8080` with Swagger documentation at `http://localhost:8080/docs`.

### Environment Variables

Configure the application using these environment variables:

| Variable | Description | Required | Default |
|----------|-------------|----------|---------|
| `DATABASE_URL` | JDBC connection URL | Yes | - |
| `DATABASE_USER` | Database username | Yes | - |
| `DATABASE_PASSWORD` | Database password | Yes | - |
| `DATABASE_DRIVER` | JDBC driver class | No | Auto-detected |
| `LOGBACK_LEVEL` | Applciation logging level (DEBUG, INFO, WARN, ERROR) | No | INFO |
| `BASIC_CLIENT_SECRET` | Secret key for authentication | Yes | - |
| `BASIC_SIGNING_SECRET` | Secret key for JWT token creation and validation | Yes | - |

### Database Support

Oni supports two database backends:

**PostgreSQL** (recommended):
```bash
docker run -d \
  -p 8080:8080 \
  -e BASICJWT_CLIENT_SECRET="your_client_secret" \
  -e BASICJWT_SIGNING_SECRET="your_signing_secret" \
  -e DATABASE_URL="jdbc:postgresql://postgres:5432/oni" \
  -e DATABASE_USER="oni" \
  -e DATABASE_PASSWORD="secret" \
  --link postgres:postgres 
  mbari/oni:latest
```

**SQL Server**:
```bash
docker run -d \
  -p 8080:8080 \
  -e BASICJWT_CLIENT_SECRET="your_client_secret" \
  -e BASICJWT_SIGNING_SECRET="your_signing_secret" \
  -e DATABASE_URL="jdbc:sqlserver://sqlserver:1433;databaseName=oni;encrypt=false" \
  -e DATABASE_USER="sa" \
  -e DATABASE_PASSWORD="YourPassword123" \
  --link sqlserver:sqlserver \
  mbari/oni:latest
```

### Building from Source

To build your own Docker image:

```bash
# Build the application
sbt 'Docker / stage'

# Build the Docker image
cd oni/target/docker/stage
docker build -t oni:local .

# Run the locally built image
docker run -d -p 8080:8080 \
  -e BASICJWT_CLIENT_SECRET="your_client_secret" \
  -e BASICJWT_SIGNING_SECRET="your_signing_secret" \
  -e DATABASE_URL="your_jdbc_url" \
  -e DATABASE_USER="user" \
  -e DATABASE_PASSWORD="pass" \
  oni:local
```

### Multi-Architecture Support

Docker images are built for both AMD64 and ARM64 architectures, supporting deployment on:
- x86_64 servers
- Apple Silicon (M1/M2/M3) Macs
- ARM-based cloud instances

## Documentation

<https://mbari-org.github.io/oni/docs>
