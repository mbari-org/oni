# API Reference

All endpoints are prefixed with `/v1` (configurable via `HTTP_CONTEXT_PATH`). The interactive Swagger UI is available at `/v1/docs` when the server is running.

Read-only endpoints require no authentication. Write endpoints (`POST`, `PUT`, `DELETE`) require a `Bearer` JWT in the `Authorization` header. See [Usage](../usage/usage.md) for how to obtain a token.

---

## Health

| Method | Path         | Auth | Description         |
|--------|--------------|------|---------------------|
| GET    | `/v1/health` | —    | Liveness/health check |

---

## Authorization

| Method | Path             | Auth | Description                                      |
|--------|------------------|------|--------------------------------------------------|
| POST   | `/v1/auth`       | —    | Exchange API key (`Authorization: APIKEY <key>`) for a JWT |
| POST   | `/v1/auth/login` | —    | Login with username/password; returns a JWT      |

---

## Concepts

Individual nodes in the knowledgebase tree.

| Method | Path                         | Auth | Description                                |
|--------|------------------------------|------|--------------------------------------------|
| GET    | `/v1/concept`                | —    | List all concept names                     |
| GET    | `/v1/concept/{name}`         | —    | Find a concept by name                     |
| GET    | `/v1/concept/find/{name}`    | —    | Find concepts whose name contains `{name}` |
| GET    | `/v1/concept/parent/{name}`  | —    | Get the parent of a concept                |
| GET    | `/v1/concept/children/{name}`| —    | Get the children of a concept              |
| GET    | `/v1/concept/query/root`     | —    | Get the root concept                       |
| GET    | `/v1/concept/ranks`          | —    | List valid taxonomic ranks                 |
| POST   | `/v1/concept`                | JWT  | Create a new concept                       |
| PUT    | `/v1/concept/{name}`         | JWT  | Update a concept                           |
| DELETE | `/v1/concept/{name}`         | JWT  | Delete a concept                           |

---

## Concept Names

Each concept may have multiple names. Name types are `PRIMARY`, `SYNONYM`, `COMMON`, and `FORMER`.

| Method | Path                | Auth | Description                         |
|--------|---------------------|------|-------------------------------------|
| GET    | `/v1/names`         | —    | List all concept names (paginated)  |
| GET    | `/v1/names/{name}`  | —    | Find a specific concept name        |
| POST   | `/v1/names`         | JWT  | Add a name to a concept             |
| PUT    | `/v1/names/{name}`  | JWT  | Update a concept name               |
| DELETE | `/v1/names/{name}`  | JWT  | Remove a concept name               |

---

## Phylogeny

Tree traversal operations. Backed by an in-memory Caffeine cache for fast response times.

| Method | Path                        | Auth | Description                                        |
|--------|-----------------------------|------|----------------------------------------------------|
| GET    | `/v1/phylogeny/up/{name}`   | —    | Branch from concept up to the root                 |
| GET    | `/v1/phylogeny/down/{name}` | —    | Branch from concept down to all leaves             |
| GET    | `/v1/phylogeny/basic/{name}`| —    | Ancestor path without child expansion              |
| GET    | `/v1/phylogeny/taxa/{name}` | —    | All taxa (leaf nodes) under a concept              |
| GET    | `/v1/phylogeny/siblings/{name}` | — | Concepts sharing the same parent               |

---

## Link Templates

Named relationship patterns that define what associations are valid for a concept.

| Method | Path                                              | Auth | Description                                              |
|--------|---------------------------------------------------|------|----------------------------------------------------------|
| GET    | `/v1/linktemplates`                               | —    | List all link templates (paginated)                      |
| GET    | `/v1/linktemplates/count`                         | —    | Count all link templates                                 |
| GET    | `/v1/linktemplates/{id}`                          | —    | Find a link template by ID                               |
| GET    | `/v1/linktemplates/concept/{conceptName}`         | —    | All link templates for a concept                         |
| GET    | `/v1/linktemplates/concept/count/{conceptName}`   | —    | Count link templates for a concept                       |
| GET    | `/v1/linktemplates/toconcept/{toConcept}`         | —    | All link templates targeting a specific concept          |
| GET    | `/v1/linktemplates/toconcept/count/{toConcept}`   | —    | Count link templates by toConcept                        |
| GET    | `/v1/linktemplates/query/for/{conceptName}`       | —    | Link templates usable for a concept (inherited + own)    |
| GET    | `/v1/linktemplates/query/for/{conceptName}/using/{linkName}` | — | Filter by link name                       |
| POST   | `/v1/linktemplates`                               | JWT  | Create a link template                                   |
| POST   | `/v1/linktemplates/prototype`                     | —    | Find link templates matching a prototype                 |
| PUT    | `/v1/linktemplates/{id}`                          | JWT  | Update a link template                                   |
| PUT    | `/v1/linktemplates/toconcept/rename`              | JWT  | Bulk rename all `toConcept` values                       |
| DELETE | `/v1/linktemplates/{id}`                          | JWT  | Delete a link template                                   |

---

## Link Realizations

Concrete instances of link templates attached to a concept.

| Method | Path                                    | Auth | Description                                   |
|--------|-----------------------------------------|------|-----------------------------------------------|
| GET    | `/v1/linkrealizations`                  | —    | List all link realizations (paginated)        |
| GET    | `/v1/linkrealizations/count`            | —    | Count all link realizations                   |
| GET    | `/v1/linkrealizations/{id}`             | —    | Find a link realization by ID                 |
| GET    | `/v1/linkrealizations/concept/{name}`   | —    | All link realizations for a concept           |
| POST   | `/v1/linkrealizations`                  | JWT  | Create a link realization                     |
| POST   | `/v1/linkrealizations/prototype`        | —    | Find link realizations matching a prototype   |
| PUT    | `/v1/linkrealizations/{id}`             | JWT  | Update a link realization                     |
| DELETE | `/v1/linkrealizations/{id}`             | JWT  | Delete a link realization                     |

---

## Links (Cross-cutting Queries)

| Method | Path                                              | Auth | Description                                          |
|--------|---------------------------------------------------|------|------------------------------------------------------|
| GET    | `/v1/links`                                       | —    | List all link templates                              |
| GET    | `/v1/links/{name}`                                | —    | All link templates applicable to a concept           |
| GET    | `/v1/links/{name}/using/{linkName}`               | —    | Link templates for a concept filtered by link name   |
| GET    | `/v1/links/query/linkrealizations/{linkName}`     | —    | All link realizations with a given link name         |

---

## Media

Image and video records associated with concepts.

| Method | Path                              | Auth | Description                         |
|--------|-----------------------------------|------|-------------------------------------|
| GET    | `/v1/media/{id}`                  | —    | Find a media record by ID           |
| GET    | `/v1/media/search/concept/{name}` | —    | All media for a concept             |
| POST   | `/v1/media`                       | JWT  | Create a media record               |
| PUT    | `/v1/media/{id}`                  | JWT  | Update a media record               |
| DELETE | `/v1/media/{id}`                  | JWT  | Delete a media record               |

---

## References

Bibliographic references optionally linked to concepts.

| Method | Path                             | Auth | Description                              |
|--------|----------------------------------|------|------------------------------------------|
| GET    | `/v1/references`                 | —    | List all references (paginated)          |
| GET    | `/v1/references/{id}`            | —    | Find a reference by ID                   |
| POST   | `/v1/references/query/citation`  | —    | Find references by citation glob pattern |
| POST   | `/v1/references`                 | JWT  | Create a reference                       |
| PUT    | `/v1/references/{id}`            | JWT  | Update a reference                       |
| DELETE | `/v1/references/{id}`            | JWT  | Delete a reference                       |

---

## History

Every write operation creates a history record. Non-admin changes are initially `PENDING` and must be approved.

| Method | Path                         | Auth | Description                                   |
|--------|------------------------------|------|-----------------------------------------------|
| GET    | `/v1/history/pending`        | JWT  | List pending change requests (paginated)      |
| GET    | `/v1/history/pending/count`  | JWT  | Count pending change requests                 |
| GET    | `/v1/history/approved`       | JWT  | List approved changes (paginated)             |

---

## User Accounts

| Method | Path                     | Auth | Description                         |
|--------|--------------------------|------|-------------------------------------|
| GET    | `/v1/users`              | JWT  | List all user accounts              |
| GET    | `/v1/users/{name}`       | JWT  | Find a user by username             |
| GET    | `/v1/users/role/{role}`  | JWT  | Find all users with a given role    |
| POST   | `/v1/users`              | JWT  | Create a user account               |
| PUT    | `/v1/users/{name}`       | JWT  | Update a user account               |
| DELETE | `/v1/users/{name}`       | JWT  | Delete a user account               |

Valid roles: `readonly`, `annotator`, `maintenance`, `admin`.

---

## Preferences

Key/value configuration store.

| Method | Path         | Auth       | Description                                         |
|--------|--------------|------------|-----------------------------------------------------|
| GET    | `/v1/prefs`  | JWT (admin)| List all preference nodes, or filter by `?name=&key=`|

---

## Raw (Database Format)

Low-level access returning entities in their database representation.

| Method | Path                     | Auth | Description                              |
|--------|--------------------------|------|------------------------------------------|
| GET    | `/v1/raw/concept/{name}` | —    | Find a concept in raw database format    |
| GET    | `/v1/raw/names/{name}`   | —    | All raw concept names for a concept      |

---

## Metrics

| Method | Path           | Auth | Description               |
|--------|----------------|------|---------------------------|
| GET    | `/v1/metrics`  | —    | Prometheus metrics scrape |
