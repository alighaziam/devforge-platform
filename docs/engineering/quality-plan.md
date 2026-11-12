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

Every case carries a stable ID, priority, source specification path, steps, and
expected result. The plan is sorted deterministically and includes category
counts. These are test intentions, not evidence that the tests have run.

## Current limitations

The first version emits a reviewable test-plan JSON response. It does not yet
generate executable test code, provision test data, run tests, calculate
coverage, or store execution evidence. Those capabilities belong to the
quality worker and isolated runtime-validation milestones.
