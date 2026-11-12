# Local Development

## Prerequisites

- JDK 25
- Maven 3.9+
- Docker Engine with Docker Compose
- Git

## Start PostgreSQL

From the repository root:

```bash
docker compose up -d postgres
docker compose ps
```

The Compose service creates a local database named `devforge`. Its default
credentials are for local development only. Change them before exposing the
database beyond your workstation.

## Configure the platform process

Spring Boot reads environment variables, not the `.env` file automatically.
Set the values in your shell or IDE run configuration:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/devforge
export DATABASE_USERNAME=devforge
export DATABASE_PASSWORD=devforge-local
export SERVER_PORT=8080
```

On Windows PowerShell:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/devforge"
$env:DATABASE_USERNAME = "devforge"
$env:DATABASE_PASSWORD = "devforge-local"
$env:SERVER_PORT = "8080"
```

## Build and run

```bash
mvn --batch-mode --no-transfer-progress verify
mvn -pl platform/core spring-boot:run -Dspring-boot.run.profiles=local
```

Flyway applies database migrations. Hibernate uses `ddl-auto=validate`; it does
not create or modify the schema. The test profile uses an isolated in-memory H2
database and runs the same Flyway migrations.

## Start Angular Studio

In a second terminal:

\`\`\`bash
cd studio
npm ci
npm start
\`\`\`

The Angular dev server proxies \`/api\` and \`/actuator\` to the local backend.
The \`local\` Spring profile permits unauthenticated local API access solely
for workstation development. It disables CSRF for this local-only browser
workflow; never expose this profile to a network or production environment.

## Initial API surface

- `GET /actuator/health` — public health probe.
- `POST /api/v1/specifications/validate` — validate a Project Specification.
- `POST /api/v1/projects` — create a project.
- `GET /api/v1/projects/{id}` — retrieve a project.
- `GET /api/v1/projects?page=0&size=20` — list projects.
- `POST /api/v1/projects/{id}/specifications` — create an immutable specification snapshot.
- `GET /api/v1/projects/{id}/specifications` — list snapshots.
- `GET /api/v1/catalog/technologies` — list catalog entries.
- `POST /api/v1/compatibility/evaluate` — evaluate selected technology versions.
- `POST /api/v1/projects/{id}/generations` — generate a ZIP from a stored immutable snapshot; returns SHA-256 and generation-run ID.
- `POST /api/v1/projects/{id}/model/import` — preview a JSON domain model or supported PostgreSQL DDL; does not persist until reviewed and saved.

All API routes other than the health probe require authentication. The current
HTTP Basic setup is a development security baseline; production OIDC, tenant
authorization, user lifecycle, and secret management are not implemented yet.
Do not expose this local setup to the public internet.
