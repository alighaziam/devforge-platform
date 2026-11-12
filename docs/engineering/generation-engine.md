# Deterministic Generation Engine — Initial Vertical Slice

## API

`POST /api/v1/projects/{projectId}/generations`

Request:

```json
{ "snapshotVersion": 1 }
```

The endpoint returns a ZIP archive and response headers containing the archive
SHA-256 digest and generated file count. The archive includes
`.devforge/manifest.json` with the generator ID/version, source snapshot ID,
specification version, source specification digest, and generated file list.

## Supported stack

Generator `devforge.generator.java-spring-postgres@0.1.0` currently supports:

- Java 25
- Spring Boot 4.0.0
- PostgreSQL 17
- Layered or modular-monolith architecture styles
- Single-project output layout
- Basic domain entities and repositories
- PostgreSQL tables and declared indexes
- Maven, Flyway, Dockerfile, and Docker Compose assets

The generator rejects unsupported technology selections, authentication
profiles, relationships, defaults, attribute types, and output layouts rather
than silently ignoring them. This first version does not generate Angular code,
REST CRUD endpoints, workflow integrations, or identity provider configuration.

## Determinism and safety

Generation consumes an immutable, schema-validated snapshot. Compatibility is
evaluated before generation; blocking or review-required findings prevent the
archive from being produced. File names and identifiers are normalized and
validated; the generator does not execute user-provided code or shell commands.
ZIP entries are sorted and timestamp-normalized so identical inputs and
generator versions produce reproducible archives.

Generated output is a starting point, not a production deployment. The first
slice does not yet build the generated archive in a sandbox or run its tests.
That is the next quality-engine milestone. Review secrets, database credentials,
security, migrations, and operational settings before deployment.
