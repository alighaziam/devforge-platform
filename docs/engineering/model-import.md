# Model Import — Initial Supported Formats

## API

`POST /api/v1/projects/{projectId}/model/import`

Request body:

```json
{
  "format": "sql-ddl",
  "content": "CREATE TABLE customer (id UUID PRIMARY KEY, email VARCHAR(255) NOT NULL UNIQUE);"
}
```

Supported formats are `json` and `sql-ddl`. The endpoint returns a normalized
model preview with entities, relationships, indexes, and explicit warnings.
It does not mutate the project or save a Project Specification snapshot. The
user reviews and merges the preview before saving.

## SQL DDL subset

The importer accepts PostgreSQL-style `CREATE TABLE`, `CREATE INDEX`, and
`CREATE UNIQUE INDEX` statements. Supported column types include UUID,
VARCHAR/CHAR, TEXT, INTEGER/SMALLINT, BIGINT, NUMERIC/DECIMAL, BOOLEAN, DATE,
and TIMESTAMP. It recognizes NOT NULL, UNIQUE, and a single UUID primary key
named `id`. Indexes are checked against imported table and column names.

The importer rejects foreign keys, composite primary keys, CHECK constraints,
DEFAULT expressions, schema-qualified identifiers, unsupported SQL types,
ALTER/DROP statements, and other unrecognized syntax. It never executes SQL.

## JSON model import

JSON input must be an object with an `entities` array. Optional
`relationships` and `indexes` values must be arrays. The importer preserves
the submitted model object and adds missing empty arrays.

## Safety and limitations

Input is limited to 512 KiB. SQL comments are removed before parsing, and
identifiers are restricted to ASCII letters, digits, and underscores. This is
a bounded metadata parser, not a general SQL parser. Unsupported constructs are
rejected instead of being silently discarded. PDF, DOCX, OpenAPI, XML, and
spreadsheet extraction are not yet implemented.


## Deterministic model diff preview

`POST /api/v1/projects/{projectId}/model/diff`

The endpoint compares two domain-model snapshots without changing the project
or persisting either snapshot:

```json
{
  "before": {
    "entities": [
      {"name": "Customer", "attributes": [{"name": "email", "type": "string"}]}
    ],
    "relationships": [],
    "indexes": []
  },
  "after": {
    "entities": [
      {"name": "Customer", "attributes": [
        {"name": "email", "type": "string", "unique": true},
        {"name": "displayName", "type": "string"}
      ]}
    ],
    "relationships": [],
    "indexes": []
  }
}
```

The response contains a stable, path-sorted list of `added`, `removed`, and
`modified` changes. Entity identity is its name; attribute identity is its
name within the entity; relationship identity is its name (or its
from/to/cardinality tuple); index identity is its name (or entity/columns).
Changed entity metadata and nested attributes are reported separately.
JSON Pointer path segments are escaped. Duplicate identities and malformed
collections are rejected rather than producing ambiguous results.

Both snapshots together are limited to 512 KiB. This is a read-only preview:
it does not merge models, alter a Project Specification, or save a snapshot.
