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
docs/                 Product, architecture, security, roadmap, ADRs
schemas/              Project Specification and plugin contracts
catalogs/             Technology and compatibility metadata
platform/             Java 25 / Spring Boot platform modules
studio/               Angular engineering studio
generators/           Stack-specific project generators
plugins/              Optional integrations
infrastructure/       Local development and deployment assets
tests/                Contract, integration, and end-to-end tests
```

## Current implementation

The initial backend vertical slice now includes:

- Java 25 / Spring Boot 4 platform core with authenticated-by-default API security.
- Draft 2020-12 Project Specification validation.
- PostgreSQL-backed project lifecycle and immutable, versioned specification snapshots.
- Versioned technology catalog read APIs and deterministic compatibility evaluation.
- Flyway migrations, Docker Compose development database, and Java 25 CI verification.

This is an actively developed foundation, not a production-ready application factory yet. Angular Studio, tenant-aware identity, model import, architecture analysis, deterministic generators, sandbox execution, and delivery automation remain on the roadmap. See [Local Development](docs/engineering/local-development.md), [Product Requirements](docs/product/requirements.md), [System Architecture](docs/architecture/system-architecture.md), and [Roadmap](docs/roadmap/roadmap.md).

## Development policy

- Do not introduce a technology into a generated project unless it is selected in that project's specification.
- Do not silently override user architecture decisions. Record compatibility findings, risks, and overrides.
- Never claim a build, test, or runtime validation passed unless it was actually executed.
- Never execute generated code on the host without sandbox isolation and explicit resource/network controls.
- Keep secrets out of source control. Use placeholders and documented secret injection.
