# Architecture Advisor — Deterministic Rule Set

## API

`POST /api/v1/projects/{projectId}/architecture/evaluate`

Request:

```json
{ "snapshotVersion": 1 }
```

The advisor evaluates an immutable Project Specification snapshot and returns
the selected style, a recommended style, rationale, findings, trade-offs, and
an ADR draft. It does not change the approved specification or persist the ADR.
The owner reviews the output and explicitly decides whether to adopt it.

## Initial rules

- Critical/high security requirements without an authentication strategy are
  blocked for review.
- MFA requirements without an identity-provider mode that can declare MFA are
  flagged.
- High availability without observability selections is flagged.
- Event-driven architecture without a broker is blocked.
- Event-driven architecture without outbox/inbox or Saga strategy is warned.
- Microservices without evidence of multiple bounded contexts are challenged.
- High cost pressure is surfaced when microservices are selected.
- High scalability with a plain layered style triggers a module-boundary warning.
- Missing quality attributes are called out as an evidence gap.

## Recommendation baseline

The initial advisor prefers a modular monolith when the model is small or
bounded contexts are not yet established. It retains the selected style when
the specification does not provide sufficient evidence to recommend a change.
This is a transparent heuristic, not an autonomous architecture decision.

## Limitations

The current rules are deterministic and intentionally small. They do not yet
model workload profiles, team topology, detailed SLOs, cloud cost, data
residency, failure-domain design, or technology-specific capacity limits.
Findings include affected specification paths and remediation so the rule set
can evolve without hiding assumptions.
