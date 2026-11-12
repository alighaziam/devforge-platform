# Project Specification Validation

## Contract source

The canonical contract is
`schemas/project-specification/project-specification.schema.json`.
The platform packages that file as a classpath resource; it is not copied into
the Java module. This keeps one versioned source of truth for tooling and runtime.

The schema uses JSON Schema Draft 2020-12. The validator selects that dialect
explicitly and validates the submitted JSON instance against the packaged schema.

## API

`POST /api/v1/specifications/validate`

- Content type: `application/json`
- `200 OK`: valid specification; response contains `valid: true` and an empty `errors` array.
- `422 Unprocessable Entity`: syntactically valid JSON that violates the schema; response contains `valid: false` and validation messages.
- `400 Bad Request`: malformed JSON; response contains a generic error without echoing the submitted payload.
- `413 Payload Too Large`: request bodies larger than 1 MiB are rejected before JSON parsing.

The endpoint inherits the platform's authenticated-by-default security policy.
Only the health probe is public at this stage.

## Design constraints

- Validation is deterministic and does not invoke an LLM.
- The endpoint does not persist or mutate the submitted specification.
- Validation messages are sorted to make responses stable for clients and tests.
- The API reports schema validity only. Cross-field business rules, technology
  compatibility, authorization, and immutable project snapshots are separate
  capabilities and must not be implied by this endpoint.
- The schema remains versioned independently; future incompatible contracts
  require an explicit versioning and migration strategy.
