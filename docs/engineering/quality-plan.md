# Requirements-Driven Quality Plan

## API

`POST /api/v1/projects/{projectId}/quality/plan`

Request: `{"snapshotVersion":1}`

The service creates a test-plan preview from a saved immutable specification.
It does not inspect generated source code and does not execute tests.

## Test sources

- Functional requirements and each acceptance criterion produce independent functional test cases.
- Required and unique entity attributes produce validation and integrity cases.
- Declared indexes produce migration/catalog/query-plan verification cases.
- Quality attributes produce measurable test objectives for performance, availability, scalability, security, usability, observability, and other quality goals.
- Project constraints produce explicit compliance checks.
- Selected authentication and MFA requirements produce security cases.

Every case carries a snapshot-scoped unique ID, priority, source specification
path, steps, and expected result. Source paths use JSON array indexes so they
can be resolved against the exact immutable snapshot. The plan is sorted
deterministically, includes category counts, and exposes a SHA-256 fingerprint
over the ordered test cases. The fingerprint is stable across repeated
generation for the same snapshot content. These are test intentions, not
evidence that the tests have run.

Coverage includes functional acceptance criteria, entity creation and
attribute constraints, relationship cardinality, declared indexes,
quality-attribute objectives, project constraints, authentication, MFA,
authorization models, and audit requirements.

## Current limitations

The first version emits a reviewable test-plan JSON response. It does not yet
generate executable test code, provision test data, run tests, calculate
coverage, or store execution evidence. Those capabilities belong to the
quality worker and isolated runtime-validation milestones.
