# Technology Catalog and Compatibility Contracts

## Canonical schemas

- Technology record: `schemas/technology-catalog/technology.schema.json`
- Compatibility rule: `schemas/compatibility/compatibility-rule.schema.json`
- Catalog index: `catalogs/catalog-index.json`
- Technology records: `catalogs/technologies/`
- Compatibility rules: `catalogs/compatibility/`

## Read API

- `GET /api/v1/catalog/technologies`
- `GET /api/v1/catalog/technologies?category=database`
- `GET /api/v1/catalog/technologies/{id}`
- `GET /api/v1/catalog/compatibility-rules`

The API exposes catalog metadata only. It does not imply that a technology is
installed, selected, supported by a generator, or compatible with another
selection. Unknown IDs return 404.

## Catalog semantics

A technology ID is globally stable and independent of its display name or
version. Each record describes one explicit version line. Lifecycle status is
not inferred from a version number. A catalog entry is metadata, not an
implicit project selection and not proof that a generator exists.

Dependencies and conflicts are expressed as technology IDs and version ranges.
The compatibility engine resolves these against selected, pinned versions.
Missing catalog entries or unknown version schemes produce an explicit
unknown finding; they must never be treated as compatible.

## Rule language

Compatibility rules are declarative JSON data. The initial expression language
supports only `allOf`, `anyOf`, `not`, `selected`, `version-in-range`, and
`provides-capability`. It does not execute scripts, expressions, templates,
shell commands, or arbitrary code from a catalog entry.

- `error + block`: selection cannot proceed without changing the configuration.
- `warning + warn`: user may proceed after reviewing the finding.
- `info + recommend`: an alternative or improvement is suggested.

Override policies are explicit. Security and hard runtime requirements use
`never`; normal architecture preferences may allow an architect or expert
approval. Every finding should retain rule ID, affected technology IDs,
severity, rationale, evidence, and remediation.

## Initial sample records

The records in this repository are starter examples, not a comprehensive
vendor support matrix. Before production use, maintainers must verify release
support windows and version ranges against primary vendor documentation and
add a catalog review date and provenance.

## Update policy

Catalog changes are version-controlled and reviewed like source code. A version
line is retired rather than silently rewritten when support status changes.
Compatibility rule changes require regression fixtures demonstrating both
matching and non-matching configurations.
