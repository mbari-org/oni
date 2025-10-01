# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Overview

Oni is a RESTful web service that provides an API for managing a tree structure of names used by annotation tools and data catalogs, typically called a "knowledgebase" (KB). Built in Scala 3.7.3, it uses Hibernate ORM with JPA for database operations, Tapir for REST API definitions, and Vert.x for the HTTP server.

## Build System & Commands

This project uses SBT (Scala Build Tool) with a multi-module structure:
- `oni` - Main application module
- `it` - Integration test base module  
- `itPostgres` - PostgreSQL integration tests
- `itSqlserver` - SQL Server integration tests

### Essential Commands

**Build and Test:**
```bash
sbt oni/compile          # Compile main application
sbt oni/test             # Run unit tests
sbt compile              # Compile all modules
sbt test                 # Run all tests
```

**Run Application:**
```bash
sbt oni/run              # Run the application locally (port 8080)
```

**Docker Build:**
```bash
./build.sh               # Cross-platform Docker build script
sbt 'Docker / stage'     # Prepare Docker build files
sbt 'Docker / publish'   # Build and publish Docker image
```

**Integration Tests:**
```bash
sbt itPostgres/test      # Run PostgreSQL integration tests
sbt itSqlserver/test     # Run SQL Server integration tests
```

**Code Quality:**
```bash
sbt scalafmtAll          # Format all Scala code
sbt scalafixAll          # Apply scalafix rules
```

## Architecture

### Core Components

**Main Entry Point:**
- `Main.scala` - Application bootstrap with Vert.x server setup
- Runs Flyway migrations on startup
- Configures endpoints and routing

**Configuration:**
- `AppConfig.scala` - Centralized configuration using Typesafe Config
- `reference.conf` - Default configuration values
- Environment variables override config values (e.g., `HTTP_PORT`, `DATABASE_URL`)

**Database Layer:**
- Hibernate ORM with JPA annotations for entity mapping
- Flyway for database migrations (`src/main/resources/db/migrations/`)
- Supports PostgreSQL and SQL Server
- Connection pooling via HikariCP

**API Layer:**
- Tapir for type-safe API definitions
- Vert.x for HTTP server implementation
- Swagger UI documentation auto-generated
- JWT authentication via Auth0
- Prometheus metrics endpoint

**Endpoint Structure:**
All endpoints are defined in `endpoints/` package:
- `AuthorizationEndpoints` - JWT token management
- `ConceptEndpoints` - Core concept CRUD operations
- `ConceptNameEndpoints` - Concept name management
- `HistoryEndpoints` - Audit trail operations
- `LinkEndpoints` - Concept link management
- `MediaEndpoints` - Media asset operations
- `PhylogenyEndpoints` - Tree structure operations
- `UserAccountEndpoints` - User management
- Plus health checks, preferences, and reference management

### Data Model

The knowledge base is modeled as a hierarchical tree structure:
- **Concept** - Core entities in the tree (can have parent/child relationships)
- **ConceptDelegate** - Proxy objects for concepts to support history tracking
- **ConceptName** - Multiple names per concept (primary, alternate, etc.)
- **LinkRealization** - Actual relationships between concepts
- **LinkTemplate** - Template definitions for relationships
- **Media** - Associated images/videos for concepts
- **History** - Audit trail of all changes
- **Reference** - Scientific references/citations
- **UserAccount** - System users and authentication

## Development Workflow

### Database Setup

The application expects either PostgreSQL or SQL Server. Configure via environment variables:
```bash
export DATABASE_DRIVER="org.postgresql.Driver"
export DATABASE_URL="jdbc:postgresql://localhost:5432/oni"
export DATABASE_USER="oni"
export DATABASE_PASSWORD="password"
```

### Running Locally

1. Set up database and configure connection
2. Run `sbt oni/run` - this will:
   - Apply Flyway migrations automatically
   - Start HTTP server on port 8080 (configurable via `HTTP_PORT`)
   - Expose Swagger UI at `/docs`
   - Expose metrics at `/metrics`

### Testing Strategy

- Unit tests use MUnit framework
- Integration tests use Testcontainers for real database testing
- Tests are located in `src/test/scala/` with mirrored package structure
- Use `MUNIT_FLAKY_OK=true` environment variable for flaky test tolerance

### Key Configuration Points

- JWT configuration: `BASICJWT_ISSUER`, `BASICJWT_CLIENT_SECRET`, `BASICJWT_SIGNING_SECRET`
- Thread pool size: `DATABASE_THREADS` (default 16)
- HTTP settings: `HTTP_PORT`, `HTTP_STOP_TIMEOUT`, `HTTP_CONTEXT_PATH`

### Important Files to Know

- `build.sbt` - Multi-module SBT configuration
- `project/Dependencies.scala` - All dependency versions centralized
- `Dockerfile` - Production container configuration
- `.github/workflows/test.yml` - CI configuration
- `oni/src/main/resources/reference.conf` - Default application configuration