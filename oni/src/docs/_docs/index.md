# oni

![MBARI logo](images/logo-mbari-3b.png)

Oni is a RESTful web service for managing hierarchical trees of scientific organism names used by annotation tools and data catalogs. We typically call this tree structure a "knowledgebase" (aka "KB"). Tree structures are useful for modeling organism phylogeny. At MBARI, we also include other terms, such as geological features and equipment used for research.

It is a replacement for [vars-kb-server](https://github.com/mbari-org/vars-kb-server) and [vars-user-server](https://github.com/mbari-org/vars-user-server).

Individual nodes in the KB are called _concepts_. Each concept may have one primary name (e.g. the accepted taxa name) and zero or more alternative names (such as synonyms, common names, or former taxa names).

## Tech Stack

| Component      | Technology                              |
|----------------|-----------------------------------------|
| Language       | Scala 3 / Java                          |
| HTTP Server    | Vert.x (reactive, non-blocking)         |
| API Definition | Tapir (type-safe, OpenAPI generation)   |
| ORM            | Hibernate / JPA                         |
| Databases      | PostgreSQL, SQL Server                  |
| Migrations     | Flyway                                  |
| Connection Pool| HikariCP                                |
| Caching        | Caffeine                                |
| JSON           | Circe                                   |
| Authentication | JWT (Auth0 Java JWT)                    |
| Metrics        | Prometheus                              |
| Build          | SBT                                     |

## Architecture

Oni uses a layered architecture with clear separation of concerns:

```
HTTP (Vert.x) → Endpoints (Tapir) → Services (Scala) → Repositories (JPA) → Database
```

- **Endpoints** — Tapir definitions for each API group; handle authentication, input validation, and serialization
- **Services** — Business logic, caching, transaction management
- **JPA Entities** — Hibernate-mapped domain objects (ConceptEntity, ConceptNameEntity, etc.)
- **Flyway** — Runs schema migrations automatically on startup

## Domain Model

- **Concept** — A node in the taxonomy tree with an optional rank, Aphia ID, and parent reference
- **ConceptName** — A name attached to a concept; types are `PRIMARY`, `SYNONYM`, `COMMON`, `FORMER`
- **LinkTemplate** — A named relationship pattern (linkName → toConcept + linkValue)
- **LinkRealization** — A concrete instance of a LinkTemplate attached to a concept
- **Media** — Image or video records associated with a concept
- **Reference** — Bibliographic citation with optional DOI
- **History** — Full audit trail; every modification creates a timestamped record
- **UserAccount** — User with role-based access (readonly, annotator, maintenance, admin)
- **PrefNode** — Key/value preference store

## API Overview

All endpoints are rooted at `/v1`. Interactive documentation is available at `/v1/docs` once the server is running.

| Group             | Base Path            | Description                          |
|-------------------|----------------------|--------------------------------------|
| Health            | `/v1/health`         | Liveness check                       |
| Authorization     | `/v1/auth`           | JWT issuance via API key or login    |
| Concepts          | `/v1/concept`        | Concept CRUD and tree navigation     |
| Concept Names     | `/v1/names`          | Name management per concept          |
| Link Templates    | `/v1/linktemplates`  | Relationship template CRUD           |
| Link Realizations | `/v1/linkrealizations`| Relationship instance CRUD          |
| Links (query)     | `/v1/links`          | Cross-cutting link queries           |
| Phylogeny         | `/v1/phylogeny`      | Tree traversal (up/down/taxa/siblings)|
| Media             | `/v1/media`          | Image/video record CRUD              |
| References        | `/v1/references`     | Bibliographic reference CRUD         |
| History           | `/v1/history`        | Audit trail and change approval      |
| Users             | `/v1/users`          | User account management              |
| Preferences       | `/v1/prefs`          | Key/value preference store           |
| Raw               | `/v1/raw`            | Low-level database-format access     |
| Metrics           | `/v1/metrics`        | Prometheus metrics                   |
