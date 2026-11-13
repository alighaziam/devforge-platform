# Project Lifecycle and Specification Snapshots

## Persistence

PostgreSQL is the platform metadata store. Flyway owns schema evolution;
Hibernate runs with `ddl-auto=validate` and must not create or mutate schemas.
The local Docker Compose profile provides a development database only. All
non-local deployments must provide database credentials through environment
variables or a secret manager.

## API

- `POST /api/v1/projects` — create a project.
- `GET /api/v1/projects/{id}` — retrieve project metadata.
- `GET /api/v1/projects?page=0&size=20&query=&status=` — list projects
  (maximum page size 100). `query` matches name or description. `status` is an
  optional uppercase status code.
- `PUT /api/v1/projects/{id}` — rename a project. The body includes `name`,
  `description`, and the current `version`. A stale version returns 409.
- `DELETE /api/v1/projects/{id}` — delete a project together with its snapshots,
  generation runs, and architecture decisions.
- `POST /api/v1/projects/{id}/specifications` — validate and persist an immutable
  Project Specification snapshot.
- `GET /api/v1/projects/{id}/specifications` — list immutable snapshots.
- `GET /api/v1/projects/{id}/specifications/{version}` — read one stored snapshot,
  including its exact specification document. Reading does not create a version.
- `GET /api/v1/projects/{id}/specifications/{version}/requirements` — read the
  functional requirements, quality attributes, constraints, and source-artifact
  metadata stored in that snapshot. Missing arrays are returned empty.

Snapshot creation validates the payload before persistence, takes a pessimistic
lock on the owning project to serialize version assignment, increments the
per-project snapshot version, and stores the exact submitted JSON plus its
SHA-256 digest. A later change creates a new row; no update or delete endpoint
exists for snapshots.

## Consistency and limitations

The database uniqueness constraint on `(project_id, snapshot_version)` is the
final guard against duplicate version numbers. Snapshot content is retained as
text to preserve the exact submitted representation; the canonical schema
version and content digest are stored separately.

Project rows store an owner subject. Reads, updates, deletes, and nested
project APIs are limited to that subject. Requirements and source-artifact
metadata live inside the immutable specification; editing them means saving a
new snapshot. The platform does not yet store uploaded artifact bytes, soft
deletion, or retention policies. The default profile still uses HTTP Basic, and
the WSO2 profile is the deployed identity boundary.
