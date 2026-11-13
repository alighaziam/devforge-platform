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

This first lifecycle slice does not yet implement tenant ownership, user/role
authorization, soft deletion, retention policies, or artifact upload. The
current HTTP Basic security chain is a development skeleton, not a production
identity solution. These limitations must be resolved before multi-user
production deployment.
