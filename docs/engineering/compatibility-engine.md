# Compatibility Engine

## API

`POST /api/v1/compatibility/evaluate`

Request body:

```json
{
  "selections": {
    "framework.spring-boot": "4.0.0",
    "language.java": "25"
  }
}
```

The response contains an overall compatibility flag, a review flag, and
structured findings. Each finding retains a rule/dependency identifier,
status, severity, affected technology IDs, rationale, remediation, evidence,
and override policy.

## Evaluation semantics

- Required dependencies must be selected and satisfy their declared version range.
- Selected conflicts block the configuration.
- Custom rules are evaluated only when their `appliesTo` selector matches.
- Unsupported version syntax or unknown technology IDs produce
  `INDETERMINATE`; unknown is never silently treated as compatible.
- `allOf`, `anyOf`, `not`, `selected`, `version-in-range`, and
  `provides-capability` are the only supported expression operators.
- The range matcher currently accepts numeric dot-separated versions and
  whitespace-separated comparison constraints (`>=`, `<=`, `>`, `<`, `=`).
  Prerelease/build metadata and vendor-specific schemes remain explicitly
  unsupported until their dedicated matchers are added.

The evaluator is deterministic and does not execute catalog-provided code.
A successful evaluation is limited to the catalog data and rule set currently
loaded by this platform version.
