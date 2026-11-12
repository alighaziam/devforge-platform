# Generator Plugin Contract

## Purpose

The generator contract separates project generation from the platform workflow. The platform selects a generator from the approved project specification, validates the generator's output, and records the exact generator identity and version in the generation manifest and audit record.

## Built-in generator API

A trusted in-process generator implements `GeneratorPlugin`:

- `descriptor()` returns a stable plugin ID, version, description, supported specification versions, and exact supported technology versions.
- `supports(specification)` is a deterministic, side-effect-free capability check. It must reject unsupported technology versions, architecture styles, output layouts, and unsupported model features.
- `generate(specification)` returns a map of relative project paths to UTF-8 file contents. It must be deterministic for the same specification and plugin version.

The `GeneratorRegistry` discovers Spring-managed built-in implementations, rejects duplicate IDs at startup, and exposes the approved descriptors through `GET /api/v1/generators`.

Generation requests may include `generatorId`. If omitted, the registry selects the only compatible generator. If more than one generator supports the specification, the request must name one explicitly. Unknown IDs return a client error; unsupported combinations are rejected before generation.

## Output safety boundary

The platform treats plugin output as untrusted data even when the implementation is a built-in plugin. Before adding the platform-owned manifest or creating an archive, `GeneratedFileValidator` enforces:

- Relative, portable paths only; no absolute paths, drive paths, backslashes, traversal segments, control characters, or `.git` writes.
- No case-insensitive path collisions.
- No `.env` or environment-specific secret files. The documented `.env.example` template is allowed.
- The generator cannot overwrite `.devforge/manifest.json`.
- Maximum 5,000 files, 10 MiB per file, and 100 MiB total UTF-8 content.

The manifest and ZIP digest are produced by the platform after validation. Generated projects are still built and run only in the controlled CI/runtime workflow; generation itself never executes generated code.

## External plugins

This release supports trusted built-in Java plugins only. A valid external plugin manifest is metadata, not permission to execute its entrypoint.

External plugins must not be dynamically class-loaded into the platform JVM. They require a separate adapter using an isolated OCI, WASM, gRPC, or HTTP runtime, with explicit resource limits, filesystem/network policy, signature and digest verification, secret isolation, and audit evidence. Permissions such as `process.execute`, `network.egress`, or `secret.read` must be denied unless a reviewed sandbox policy explicitly grants them.

## Versioning

- Plugin IDs are immutable and globally unique within the platform registry.
- Plugin versions are independently versioned from the platform and project specification.
- A plugin must declare the specification versions and exact technology versions it supports.
- Changes to output semantics require a plugin version change.
- Generation manifests and audit records preserve the selected plugin ID and version for reproducibility.
