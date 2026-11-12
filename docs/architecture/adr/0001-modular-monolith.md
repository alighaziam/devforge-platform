# ADR-0001: Start with a modular monolith

- Status: Accepted
- Date: 2026-11-12
- Decision owners: DevForge maintainers

## Context

DevForge has multiple substantial capabilities, but the product and domain boundaries are still being discovered. Deploying many services immediately would add network failure modes, distributed transactions, operational overhead, and version coordination before independent scaling or deployment needs are proven.

## Decision

Build the initial platform as a modular monolith. Enforce module ownership, explicit contracts, dependency direction, independent tests, and no circular dependencies. Use plugin contracts and isolated runtimes for external generator/integration code.

## Consequences

### Positive
- Simpler local development, deployment, debugging, and transactional workflows.
- Easier cross-module refactoring while domain boundaries mature.
- Lower initial operational burden.
- Plugin execution remains isolated without making every core capability a network service.

### Negative
- Requires discipline to prevent package and persistence coupling.
- Large deployments may eventually need selective module extraction.
- A shared release train initially couples platform module releases.

## Revisit when

A module has independent scaling, security isolation, release cadence, availability, or team-ownership requirements that justify service extraction.
