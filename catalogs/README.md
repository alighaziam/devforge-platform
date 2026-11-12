# Technology Catalog

The catalog is machine-readable metadata used by the Studio, compatibility engine, architecture advisor, and generator planner.

## Design rules

- Each technology has a stable ID, category, vendor/project, version, lifecycle status, supported platforms, and documentation references.
- Version support is explicit. Never infer compatibility from a technology name alone.
- Dependencies and conflicts are declarative constraints.
- A technology being present in the catalog does not mean it is selected for a project.
- Compatibility rules are version-scoped and explainable.
- Deprecated and end-of-life versions remain identifiable for migration analysis but are not recommended for new projects.

## Proposed record

```json
{
  "id": "database.postgresql",
  "category": "database",
  "name": "PostgreSQL",
  "version": "17.x",
  "status": "supported",
  "capabilities": ["relational", "json", "transactions"],
  "requires": [],
  "conflicts": [],
  "constraints": [],
  "documentation": ["https://www.postgresql.org/docs/"]
}
```

## Compatibility rule model

A rule should contain: stable ID, severity, affected technology selectors, predicate, explanation, evidence/reference, remediation, and whether expert override is allowed. Hard security constraints cannot be overridden by a normal architecture preference.

Initial catalog data must be reviewed and version-pinned before it is used to generate production projects.
