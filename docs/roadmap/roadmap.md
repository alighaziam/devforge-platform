# Roadmap

## Phase 0 — Foundation

- [x] Confirm product direction and repository.
- [x] Establish repository documentation baseline.
- [x] Define Project Specification schema and runtime validation.
- [x] Define technology catalog and compatibility rule contracts.
- [x] Add CI quality gates and repository contribution standards.
- [x] Define plugin manifest trust, signing, and capability policy.
- [x] Define module boundaries and dependency rules.
- [x] Add architecture tests preventing module cycles.

**Exit criteria:** core contracts are reviewable, versioned, and internally consistent.

## Phase 1 — Platform skeleton

- [x] Create Java 25 / Spring Boot multi-module Maven build.
- [x] Create Angular 20 Studio workspace for project lifecycle, specification validation, catalog browsing, and compatibility analysis.
- [x] Add health/readiness endpoints and graceful shutdown configuration.
- [x] Add CI pipeline.
- [x] Add architecture tests preventing module cycles.
- [x] Add API contract and schema validation tests.

**Exit criteria:** reproducible build and CI for platform skeleton.

## Phase 2 — Project and specification core

- [x] Project create/read/list lifecycle and PostgreSQL persistence.
- [x] Specification schema validation API.
- [x] Immutable, versioned specification snapshots with SHA-256 provenance.
- [ ] Requirements and source-artifact metadata lifecycle.
- [x] Technology catalog read API.
- [x] ADR drafts, explicit decision status transitions, optimistic locking, and transition audit history.
- [x] Deterministic technology dependency and compatibility evaluation API.

**Exit criteria:** user can create a project, save a validated immutable specification snapshot, and inspect deterministic technology compatibility findings.

## Phase 3 — Model and architecture engines

- [x] Import structured domain-model JSON and a bounded PostgreSQL SQL DDL subset as reviewable previews.
- [ ] Import OpenAPI, XML, PDF, DOCX, and spreadsheet formats.
- [ ] Domain model representation and model diff.
- [x] Deterministic architecture decision/risk engine with explainable findings and ADR drafts.
- [x] Architecture Decision Records and approval lifecycle.

**Exit criteria:** model and technology choices produce explainable validation findings.

## Phase 4 — First generator vertical slice

Start with one narrow, end-to-end supported stack rather than claiming broad support prematurely. Current supported slice: Java 25 + Spring Boot 4 + PostgreSQL 17 backend; frontend generation and security integrations are not yet supported.

- [x] Deterministic generator plugin contract and registry for the first supported stack.
- [x] Generator discovery API, explicit plugin selection, and duplicate-ID rejection.
- [x] Generated artifact path, secret-file, reserved-manifest, and size validation.
- [x] Generated project manifest and provenance, with persistent generation-run audit records.
- [ ] Golden-file tests and regeneration safety.

- [x] Exit criteria: identical snapshots and generator versions produce reproducible output, verified by repeat-generation SHA-256 integration tests and a separate generated-project Maven CI build.

## Phase 5 — Quality and sandbox

- [x] Independent test-plan preview generation from requirements, domain constraints, security decisions, and quality attributes.
- [x] CI builds and smoke-runs the first generated sample with PostgreSQL and Actuator health verification.
- [ ] Hardened isolated build workers for arbitrary generated projects.
- [ ] Build, unit, integration, smoke, and API validation.
- [ ] Evidence bundle and honest skipped/failed status.

**Exit criteria:** generated artifact has traceable validation evidence.

## Phase 6 — Plugin ecosystem and delivery

- [ ] External plugin signing, verification, and sandboxed execution.
- [ ] Additional Java/Python/.NET and frontend generators.
- [ ] Git branch/PR delivery.
- [ ] CI/CD and deployment profiles.
- [ ] SBOM and supply-chain controls.

**Exit criteria:** plugins can be installed, validated, and executed under declared permissions.

## Product rule

Do not start a new phase by marking incomplete exit criteria as done. Update this roadmap with evidence from actual builds and tests.
