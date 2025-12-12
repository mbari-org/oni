# 
Oni (Organism Naming Infrastructure) - Codebase Summary

**Oni** is a RESTful web service for managing hierarchical trees of scientific organism names (taxonomic knowledgebase) for MBARI (Monterey Bay Aquarium Research Institute).

## Tech Stack
- **Languages:** Scala 3.7.3 + Java 21
- **Frameworks:** Vert.x (HTTP), Tapir (API), Hibernate/JPA (ORM)
- **Databases:** PostgreSQL & SQL Server (dual support)
- **Build:** SBT multi-module project
- **Key Libraries:** Flyway, HikariCP, Caffeine cache, Circe JSON, JWT auth

## Architecture

Layered architecture with clear separation:

`HTTP (Vert.x) → Endpoints (Tapir) → Services (Scala) → Repositories (Java) → JPA Entities → Database`

## Main Components

1. **Domain Entities** (Java/JPA): ConceptEntity, ConceptNameEntity, LinkTemplateEntity, MediaEntity, HistoryEntity, UserAccountEntity
2. **REST Endpoints** (Scala): 15+ endpoint groups for concepts, phylogeny, media, links, users, history
3. **Services** (Scala): Business logic with caching (FastPhylogenyService, ConceptService, etc.)
4. **Special Features**: Full audit trail, JWT auth, in-memory tree caching, auto-generated Swagger docs

## Project Modules

- **oni/** - Main application
- **it/** - Integration tests (database-agnostic)
- **it-postgres/** - PostgreSQL-specific tests
- **it-sqlserver/** - SQL Server-specific tests

## Key Patterns

- Functional error handling with `Either[Throwable, T]`
- Type-safe APIs via Tapir
- Repository pattern for data access
- Constructor-based dependency injection
- Comprehensive caching strategy
- Automatic Flyway migrations on startup

This is a production-ready, enterprise-grade microservice with strong emphasis on type safety, performance, and maintainability.