# Product Requirements

## 1. Product vision

DevForge is an enterprise application engineering workbench. It guides a user from requirements and domain/data models to an architecture decision, a validated technology selection, generated source code, quality evidence, and a deployable deliverable.

It is not a template picker and it is not an autonomous code generator that hides decisions. It is a specification-driven engineering system with explicit user control and verifiable outputs.

## 2. Primary user journey

1. Create a project and define its purpose, business capabilities, constraints, and non-functional requirements.
2. Import or author a domain/data model.
3. Review extracted entities, attributes, relationships, constraints, indexes, and assumptions.
4. Select architecture styles and patterns.
5. Select language, framework, database, identity provider, messaging, cache, UI, workflow, observability, build, runtime, and deployment technologies.
6. Run compatibility and architecture analysis.
7. Review recommendations, risks, alternatives, and Architecture Decision Records (ADRs).
8. Approve a versioned Project Specification.
9. Generate the selected project components.
10. Build and validate in an isolated environment.
11. Review generated source, test results, SBOM, security findings, and known limitations.
12. Export an archive or publish to a user-authorized Git repository and CI/CD target.

## 3. Functional requirements

### FR-01 Project workspace
Manage projects, versions, owners, environments, requirements, decisions, generation runs, and delivery artifacts.

### FR-02 Requirements ingestion
Accept structured and unstructured inputs including JSON, XML, SQL DDL, OpenAPI, PDF, DOCX, and plain text. Preserve source provenance and distinguish extracted facts from inferred assumptions.

### FR-03 Domain and data modeling
Represent entities, value objects, attributes, relationships, cardinality, constraints, indexes, database mappings, and bounded contexts. Support model validation, versioning, and change comparison. Provide ERD and domain views in the Studio.

### FR-04 Technology selection
Support independently selectable technology families: backend, frontend, database, build, identity, messaging, cache, workflow, reporting, observability, runtime, and deployment. A project must contain only selected components and their explicitly required dependencies.

### FR-05 Architecture design
Support layered, clean, hexagonal, modular monolith, service-based, microservices, event-driven, CQRS, and serverless styles. Permit scoped combinations and document boundaries and dependencies.

### FR-06 Architecture advice
Evaluate quality attributes, constraints, scale assumptions, operational maturity, cost, coupling, failure modes, and consistency requirements. Produce explainable findings with severity, evidence, trade-offs, alternatives, and user disposition. Do not silently change an approved decision.

### FR-07 Identity and application security
Support application security profiles including session, JWT, OAuth 2.0, OIDC, SAML, API keys, and mTLS where appropriate. Integrate selected providers such as WSO2 Identity Server and Keycloak, or internal identity where explicitly selected. Model RBAC/ABAC/ReBAC, MFA, token lifecycle, audit, secrets, encryption, and API protections.

### FR-08 Data, cache, and messaging
Model relational and document databases, schema migrations, views, indexes, query assets, connection management, cache strategies, and broker integration. Keep database state, cache, session state, and workflow state distinct.

### FR-09 UI/UX Studio
Provide page, form, navigation, theme, component, responsive layout, localization, RTL, and accessibility design. Generate only the selected frontend stack or no frontend.

### FR-10 Workflow and integration
Model BPMN/process workflows and integrations using selected engines and protocols. Support REST, GraphQL, gRPC, OpenAPI, webhooks, and event contracts.

### FR-11 Reporting and analytics
Provide report, query, chart, and dashboard definitions, export formats, schedules, drill-down, and data permissions.

### FR-12 Quality engineering
Generate test plans and tests from requirements, domain contracts, API contracts, and acceptance criteria independently of generated implementation. Support unit, integration, contract, E2E, performance, security, accessibility, migration, and smoke testing according to selected stack.

### FR-13 Runtime validation
Provision isolated dependencies, apply migrations, start generated components, and run health, smoke, API, and E2E checks. Capture logs and machine-readable results. Report skipped checks and environmental limitations.

### FR-14 Observability
Configure metrics, logs, traces, dashboards, health checks, and alerts using selected tools such as OpenTelemetry, Prometheus, Grafana, Loki, Elasticsearch, Jaeger, or Sentry.

### FR-15 Delivery
Support build/package, Docker, Compose, Kubernetes, Helm, OpenShift, VM, and cloud deployment profiles. Support CI/CD, SBOM, dependency/license/container scanning, backup/recovery, and deployment strategies.

### FR-16 Safe regeneration
Preserve user-owned changes through generated-region ownership, merge analysis, explicit conflict reporting, and reviewable patches. Never overwrite user code silently.

### FR-17 Plugin ecosystem
Provide signed/versioned plugin manifests, capability declarations, compatibility constraints, permission scopes, lifecycle hooks, and isolated execution contracts.

## 4. Non-functional requirements

- Security: least privilege, secure defaults, tenant isolation, secrets isolation, auditability, sandboxing.
- Reliability: idempotent operations, durable job state, retry policies, bounded resource consumption.
- Reproducibility: pinned toolchain versions and deterministic generation where feasible.
- Explainability: every recommendation and validation finding links to evidence and affected specification paths.
- Extensibility: plugin APIs are versioned and backward compatibility is explicit.
- Portability: platform deployment must support containerized environments.
- Accessibility: Studio targets WCAG 2.2 AA.
- Localization: Unicode and RTL support are first-class.
- Observability: platform operations emit structured logs, metrics, and traces.
- Data protection: user artifacts and generated secrets are not exposed in logs or public artifacts.

## 5. Explicit product boundaries

- DevForge platform implementation stack is Java 25, Spring Boot, Spring Security, and Angular.
- Generated application stacks are independent and user-selected.
- AI assists extraction, explanation, and recommendations; validated specifications and deterministic generators govern code output.
- No technology is mandatory in a generated project merely because DevForge itself uses it.
- A successful generation is not equivalent to a successful build; a successful build is not equivalent to runtime validation. These statuses remain separate.

## 6. Acceptance principles

A release artifact must identify its specification version, generator/plugin versions, selected toolchain, generated file manifest, build results, executed test suites, security findings, SBOM location, and unresolved limitations.
