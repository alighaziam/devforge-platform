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
- `GET /api/v1/projects?page=0&size=20` — list projects (maximum page size 100).
- `POST /api/v1/projects/{id}/specifications` — validate and persist an immutable
  Project Specification snapshot.
- `GET /api/v1/projects/{id}/specifications` — list immutable snapshots.

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
