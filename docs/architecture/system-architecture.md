# System Architecture

## 1. Architectural direction

DevForge is a **modular monolith with plugin-based extension points** for its initial platform implementation. Modules are independently owned and tested, communicate through explicit application contracts, and avoid direct access to another module's persistence model. Extraction into services is a future deployment decision driven by evidence, not a default.

## 2. Platform technology baseline

- Backend: Java 25, Spring Boot, Spring Security.
- Studio: Angular.
- Persistence: PostgreSQL is the proposed platform metadata store; final selection and version pinning belong to the platform infrastructure ADR.
- API contract: OpenAPI for HTTP APIs and versioned internal Java interfaces/events.
- Specification: JSON Schema 2020-12, with explicit semantic validation.
- Build: Maven for the Java platform baseline.
- Runtime: container-first; local development via Docker Compose when infrastructure dependencies are introduced.

These are platform choices, not defaults imposed on generated applications.

## 3. Logical modules

| Module | Responsibility |
|---|---|
| identity | Platform users, authentication integration, authorization, tenant context |
| project-management | Projects, workspaces, versions, requirements, lifecycle |
| model-engine | Import, normalize, validate, diff, and visualize domain/data models |
| technology-catalog | Versioned technology metadata and support lifecycle |
| compatibility-engine | Constraint evaluation across selected components |
| architecture-engine | Quality attribute analysis, trade-offs, ADRs, risk findings |
| specification | Canonical Project Specification, schema registry, validation |
| generator-engine | Plan, schedule, execute, and record deterministic generation |
| plugin-registry | Plugin manifests, signatures, versions, permissions, compatibility |
| quality-engine | Test plans, test execution orchestration, evidence |
| runtime-validator | Isolated build/runtime environments and resource controls |
| observability | Platform telemetry and generated-project observability profiles |
| reporting | Reports, dashboards, audit and export views |
| delivery | Archives, Git operations, CI/CD and deployment descriptors |

## 4. Dependency rules

1. Dependencies point toward stable contracts; modules do not query another module's tables.
2. Domain modules own their persistence and migrations.
3. Cross-module synchronous calls use application interfaces.
4. Cross-module asynchronous events are versioned and carry correlation and causation identifiers.
5. No circular module dependencies.
6. Plugin code is not loaded into the trusted platform process by default.
7. Generation consumes an immutable, validated specification snapshot.
8. Every generation run records exact plugin and toolchain versions.

## 5. Core flow

```text
Requirements + Source Artifacts
             |
             v
       Model Engine ----> Human Review
             |
             v
    Project Specification <---- Technology Catalog
             |
             v
 Architecture + Compatibility Analysis
             |
        ADR Approval
             |
             v
      Immutable Spec Snapshot
             |
             v
       Generation Plan
             |
             v
    Isolated Generator Runtime
             |
             v
 Build -> Tests -> Runtime Validation
             |
             v
 Evidence Bundle -> Export / Git / CI
```

## 6. Trust boundaries

- Browser/Studio to platform API.
- Uploaded documents and model files to parsers.
- LLM output to schema/semantic validators.
- Plugin packages to plugin verifier and isolated runtime.
- Generated source and build scripts to execution sandbox.
- Platform to Git provider and deployment targets.
- Tenant data to shared platform infrastructure.

All external inputs are untrusted. Parsing, generation, and execution must enforce size, time, memory, CPU, filesystem, network, and process limits.

## 7. Generation architecture

The generator engine first resolves a generation plan from the immutable specification and selected plugin versions. Each generator declares input/output contracts, capabilities, dependencies, and supported specification versions. The engine validates outputs, records provenance, and produces a manifest. Generators must not access secrets unless a declared capability explicitly requires it and the user has authorized that scope.

## 8. Validation architecture

Quality plans originate from requirements, acceptance criteria, domain constraints, and API contracts. Implementation-derived tests supplement but do not replace independent tests. Build, unit, integration, contract, security, E2E, and runtime statuses are reported separately. A release gate is policy-driven and cannot mark an unexecuted check as passed.

## 9. Persistence and versioning

Project Specification snapshots are immutable. Project metadata, model versions, ADRs, generation runs, plugin inventory, and evidence reference immutable version identifiers. Schema migrations are owned by the module that owns the data.

## 10. Deployment evolution

Start as a modular monolith. Keep boundaries and contracts service-extraction-ready, but do not introduce distributed infrastructure such as Kafka or service meshes for internal module calls without a demonstrated requirement.
