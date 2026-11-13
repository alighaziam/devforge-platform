# DevForge

**Enterprise Application Engineering Platform**

DevForge is a data-model-first, specification-driven platform for designing, generating, validating, and delivering enterprise applications across multiple technology stacks.

> The platform is built with Java 25, Spring Boot, Spring Security, and Angular. Generated applications are independent projects whose stacks are selected by the user.

## Product principles

- **Model first:** begin with domain/data models and source requirements.
- **Choice with guidance:** users choose their architecture and technologies; DevForge evaluates compatibility, risks, and trade-offs.
- **Specification driven:** a versioned, validated Project Specification is the source of truth.
- **Deterministic generation:** LLMs may assist analysis and recommendations; generators consume validated specifications and produce reproducible outputs.
- **Validate before delivery:** build, tests, migrations, runtime health, and integration checks must be reported honestly.
- **Extensible by design:** generators and integrations are versioned plugins.
- **Safe by default:** generated code and user-provided artifacts are untrusted. Arbitrary project execution is not enabled until isolated workers and resource/network controls are in place.

## Initial scope

1. Product requirements and architecture decision records.
2. Project Specification JSON Schema and technology catalog.
3. Modular-monolith platform core.
4. Model and architecture engines.
5. Plugin contracts and generator runtime.
6. Quality engineering and isolated runtime validation.
7. Delivery integrations for Git and CI/CD.

## Repository layout

```text
docs/                 Product, architecture, security, engineering, roadmap, ADRs
schemas/              Project Specification, plugin, catalog contracts
catalogs/             Versioned technology and compatibility metadata
platform/core/        Java 25 / Spring Boot modular platform core
studio/               Angular engineering studio
.github/workflows/    Java, generated-project, and Angular CI
docker-compose.yml    Local PostgreSQL development service
```

## Current implementation

The initial backend vertical slice now includes:

- Java 25 / Spring Boot 4 platform core with authenticated-by-default API security.
- Angular 20 Studio workspace for projects, specifications, catalog browsing, compatibility evaluation, and generation.
- JSON and bounded PostgreSQL DDL model-import previews.
- A deterministic architecture advisor that reports security, availability, scalability, and distributed-systems risks with trade-offs and ADR drafts.
- Persistent ADR lifecycle with explicit proposal, acceptance, rejection, supersession, optimistic locking, and transition audit history.
- Requirements-driven quality-plan previews with independent functional, data-integrity, security, and non-functional test intentions.
- A deterministic Java 25 / Spring Boot 4 / PostgreSQL 17 generator with provenance manifests and auditable generation runs.
- CI compiles the generated sample project as a separate Maven build.
- Draft 2020-12 Project Specification validation.
- PostgreSQL-backed project lifecycle and immutable, versioned specification snapshots.
- Versioned technology catalog read APIs and deterministic compatibility evaluation.
- Flyway migrations, Docker Compose development database, and Java 25 CI verification.

This is an actively developed foundation, not a production-ready application factory yet. Tenant-aware identity, model import, architecture analysis, broader generator coverage, sandboxed runtime validation, plugin execution, and delivery automation remain on the roadmap. See [Local Development](docs/engineering/local-development.md), [Product Requirements](docs/product/requirements.md), [System Architecture](docs/architecture/system-architecture.md), and [Roadmap](docs/roadmap/roadmap.md).

## Development policy

- Do not introduce a technology into a generated project unless it is selected in that project's specification.
- Do not silently override user architecture decisions. Record compatibility findings, risks, and overrides.
- Never claim a build, test, or runtime validation passed unless it was actually executed.
- Never execute generated code on the host without sandbox isolation and explicit resource/network controls.
- Keep secrets out of source control. Use placeholders and documented secret injection.


## WSO2 Identity Server authentication

The default profile retains HTTP Basic for the existing single-operator development workflow. For an environment integrated with WSO2 Identity Server, activate the `wso2` Spring profile and set:

- `WSO2_ISSUER_URI`: must exactly match the JWT `iss` claim.
- `WSO2_JWKS_URI`: the WSO2 JWKS endpoint reachable by the application.

The WSO2 profile validates bearer JWTs and applies scope-based authorization:
- `devforge.read`: GET API endpoints and API documentation.
- `devforge.write`: POST, PUT, PATCH and DELETE API endpoints.
- `devforge.admin`: actuator management endpoints.

Health probes remain public and reveal no component details. Unmatched routes are denied. The `local` profile deliberately disables authentication for isolated development; never expose it on a shared or public network. Use the `wso2` profile for deployed environments and install the WSO2 CA chain in the JVM truststore when using TLS.

WSO2 mode is exercised by `Wso2SecurityConfigurationTests`, including anonymous denial and read/write scope separation.


## Project ownership and tenant isolation

Project rows now store the authenticated owner subject. Under the WSO2 profile, the JWT `sub` (Spring Security principal name) owns the project; the same owner check is applied to project reads, updates, deletes, snapshots, and every nested project API through a request interceptor. Projects are hidden from other subjects with HTTP 404 to avoid leaking their existence.

The migration assigns existing projects to the local-development owner `local` so the existing single-machine workflow remains usable. Before enabling WSO2 for an existing installation, transfer those rows to the intended JWT subject, for example:

```sql
UPDATE projects SET owner_subject = '<your-wso2-subject>' WHERE owner_subject = 'local';
```

Do not use a shared service account subject for multiple human users. This first ownership boundary is project-level; a later phase should extend the same tenant context to organization membership, invitations, audit ownership and background job attribution.
