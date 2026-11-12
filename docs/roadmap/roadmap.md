# Roadmap

## Phase 0 — Foundation

- [x] Confirm product direction and repository.
- [x] Establish repository documentation baseline.
- [x] Define Project Specification schema and runtime validation.
- [ ] Define plugin manifest and capability model.
- [ ] Define technology catalog structure and compatibility rule format.
- [ ] Define module boundaries and dependency rules.
- [x] Add CI quality gates and repository contribution standards.

**Exit criteria:** core contracts are reviewable, versioned, and internally consistent.

## Phase 1 — Platform skeleton

- [x] Create Java 25 / Spring Boot multi-module Maven build.
- [ ] Create Angular Studio workspace.
- [x] Add health/readiness endpoints and graceful shutdown configuration.
- [x] Add CI pipeline.
- [ ] Add architecture tests preventing module cycles.
- [x] Add API contract and schema validation tests.

**Exit criteria:** reproducible build and CI for platform skeleton.

## Phase 2 — Project and specification core

- [ ] Project/workspace lifecycle and persistence.
- [x] Specification schema validation API.
- [ ] Specification versioning and immutable snapshots.
- [ ] Requirements and source-artifact metadata lifecycle.
- [ ] Technology catalog read API.
- [ ] ADR records and decision status.

**Exit criteria:** user can create a project and save a validated specification snapshot.

## Phase 3 — Model and architecture engines

- Import JSON, SQL DDL, OpenAPI, XML, and document formats incrementally.
- Domain model representation and model diff.
- Architecture decision/risk engine.
- Compatibility constraint evaluator.

**Exit criteria:** model and technology choices produce explainable validation findings.

## Phase 4 — First generator vertical slice

Start with one narrow, end-to-end supported stack rather than claiming broad support prematurely. Proposed first slice: Java/Spring Boot + PostgreSQL + Angular, with optional security profile.

- Deterministic generator contract.
- Generated project manifest and provenance.
- Golden-file tests and regeneration safety.

**Exit criteria:** same specification and generator versions produce reproducible output.

## Phase 5 — Quality and sandbox

- Independent test-plan generation from requirements/contracts.
- Isolated build workers.
- Build, unit, integration, smoke, and API validation.
- Evidence bundle and honest skipped/failed status.

**Exit criteria:** generated artifact has traceable validation evidence.

## Phase 6 — Plugin ecosystem and delivery

- Plugin registry and signing/verification.
- Additional Java/Python/.NET and frontend generators.
- Git branch/PR delivery.
- CI/CD and deployment profiles.
- SBOM and supply-chain controls.

**Exit criteria:** plugins can be installed, validated, and executed under declared permissions.

## Product rule

Do not start a new phase by marking incomplete exit criteria as done. Update this roadmap with evidence from actual builds and tests.
