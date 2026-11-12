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
- **Safe by default:** generated code and user-provided artifacts are untrusted; execution occurs in isolated sandboxes.

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
- A deterministic Java 25 / Spring Boot 4 / PostgreSQL 17 generator with provenance manifests and auditable generation runs; CI builds and smoke-runs a generated sample with PostgreSQL.
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
