# Architecture Decision Record Lifecycle

## API

- `POST /api/v1/projects/{projectId}/architecture/decisions` — create or return an ADR draft from a saved snapshot.
- `GET /api/v1/projects/{projectId}/architecture/decisions` — list ADRs and transition history.
- `POST /api/v1/projects/{projectId}/architecture/decisions/{decisionId}/transition` — transition status.

Create request: `{"snapshotVersion":1}`

Transition request: `{"targetStatus":"PROPOSED","comment":"Ready for review"}`

## State machine

- DRAFT → PROPOSED or REJECTED
- PROPOSED → ACCEPTED, REJECTED, or DRAFT
- ACCEPTED → SUPERSEDED
- REJECTED → DRAFT
- SUPERSEDED is terminal

Rejecting or superseding requires a rationale. Every transition records actor,
source/target status, comment, and timestamp. Optimistic locking prevents
silent concurrent updates. Draft creation does not approve the decision;
only an explicit ACCEPTED transition does.

Fine-grained approval roles, signatures, multi-party review, and immutable
event-log storage remain future governance work.
