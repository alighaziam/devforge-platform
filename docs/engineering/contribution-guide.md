# Engineering Contribution Guide

## Working agreements

- Keep changes focused and reviewable.
- Update schemas, docs, and ADRs when contracts or architectural decisions change.
- Do not commit secrets, personal data, production credentials, or customer artifacts.
- Prefer deterministic generation and tests over prompt-only behavior.
- Do not report checks as passing unless they were executed.

## Branch and commit conventions

- Branches: `feature/<short-name>`, `fix/<short-name>`, `docs/<short-name>`.
- Commits: Conventional Commits, e.g. `feat(model): add entity relationship validation`.
- Use pull requests for material changes; protect `main` when repository settings allow.

## Definition of done

- Scope and acceptance criteria are clear.
- Relevant unit/contract/integration tests are added.
- JSON schemas validate against a standards-compliant Draft 2020-12 validator.
- Security and compatibility implications are documented.
- Build/test evidence is attached or the unverified status is explicit.
- Documentation and roadmap status match the implementation.

## Generated code policy

Generated output must include provenance metadata and must not overwrite user-owned changes silently. Any regeneration conflict must be surfaced as a reviewable diff.
