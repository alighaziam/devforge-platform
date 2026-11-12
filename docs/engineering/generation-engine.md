# Deterministic Generation Engine — Initial Vertical Slice

## API

- `POST /api/v1/projects/{projectId}/generations` — generate a ZIP archive from a stored immutable snapshot.
- `GET /api/v1/projects/{projectId}/generations` — list generation-run audit records.

The generation response includes the archive SHA-256, generated file count, and
persistent generation-run ID. Each run records its source snapshot, generator
version, output digest, file count, and generated status. The archive includes
`.devforge/manifest.json` with the generator and source provenance.

## Supported stack

Generator `devforge.generator.java-spring-postgres@0.1.0` currently supports:

- Java 25, Spring Boot 4.0.0, PostgreSQL 17
- Layered or modular-monolith architecture styles
- Single-project output layout
- Basic domain entities and repositories
- PostgreSQL tables and declared indexes
- Maven, Flyway, Dockerfile, Docker Compose, and Actuator health endpoint

Unsupported technology selections, authentication profiles, relationships,
defaults, attribute types, and output layouts are rejected rather than silently
ignored. This first version does not generate Angular code, REST CRUD endpoints,
workflow integrations, or identity-provider configuration.

## Determinism and safety

Generation consumes an immutable, schema-validated snapshot. Compatibility is
evaluated before generation; blocking or review-required findings prevent the
archive from being produced. File names and identifiers are normalized and
validated; the generator does not execute user-provided code or shell commands.
ZIP entries are sorted and timestamp-normalized so identical inputs and
generator versions produce reproducible archives.

The platform CI extracts a generated sample, runs Maven verification, starts
PostgreSQL and the generated application with Docker Compose, and probes the
Actuator health endpoint. This is a controlled CI smoke test, not a hardened
multi-tenant sandbox for arbitrary user-generated code.

Generated output is a starting point, not a production deployment. Review
secrets, database credentials, security, migrations, and operational settings
before deployment. A hardened isolated build/runtime worker remains future work.
