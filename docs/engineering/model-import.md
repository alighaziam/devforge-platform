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

Supported formats are `json`, `sql-ddl`, and `openapi` (OpenAPI 3.0/3.1 JSON). The endpoint returns a normalized
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


## OpenAPI 3.x JSON import

The model import preview also accepts `format: "openapi"` with an OpenAPI 3.0 or 3.1 JSON document. It maps object definitions under `components.schemas` into model entities:

- Primitive properties become attributes (`uuid`, `date`, `datetime`, `string`, `integer`, `long`, `decimal`, or `boolean`).
- A property referencing another object schema becomes a `many-to-one` relationship.
- An array of references to an object schema becomes a `one-to-many` relationship.
- Inline objects and arrays without object-schema references are represented as JSON attributes.
- OpenAPI 3.1 nullable type unions are treated as optional model attributes.

Example request:

```json
{
  "format": "openapi",
  "content": "{\"openapi\":\"3.1.0\",\"components\":{\"schemas\":{\"Customer\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\",\"format\":\"uuid\"}}}}}}"
}
```

The importer is deliberately local and bounded: it never fetches remote `$ref` URLs, rejects `allOf`/`oneOf`/`anyOf` composition, and does not import `paths`, operations, request/response bodies or security schemes. These limitations are returned as preview warnings. The existing 512 KiB source limit applies, and import remains a preview; it does not save or overwrite the approved Project Specification.
