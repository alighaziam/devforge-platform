# Plugin Contract — Version 1.0.0

## Purpose

Plugins extend DevForge through explicit, versioned capabilities. A plugin is
not trusted merely because it is present in a repository or registry.

## Manifest

The canonical manifest is
`schemas/plugin-manifest/plugin-manifest.schema.json`. It declares:

- Stable plugin ID, publisher, semantic version, and kind.
- Capabilities and supported platform API / specification versions.
- Requested permission scopes.
- A process/container/remote entrypoint contract.
- Optional integrity metadata for digest and signature.

The registry must verify the package digest and publisher signature before
activation. Production policy requires a trusted signature even though local
development manifests may omit integrity metadata.

## Capability and permission rules

Capabilities describe what a plugin can do; permissions describe what it may
access. They are separate declarations. The platform grants only the
intersection of requested permissions, administrator policy, and user consent.

A plugin must not receive workspace write, network egress, secret access, or
process execution by default. Network access is deny-by-default and must be
restricted to approved destinations. Secret values are never passed to a plugin
unless a narrowly scoped workflow requires them and the user has authorized it.

## Runtime boundary

The initial supported execution boundary is an isolated worker process or OCI
container. Plugin code is not loaded into the trusted platform JVM. The runtime
must enforce CPU, memory, time, filesystem, process, and network limits and
must terminate workers that exceed them.

## Protocol evolution

- Manifest versions and platform API versions are independent.
- Plugins declare supported Project Specification versions.
- Breaking protocol changes require a new major API version and a migration path.
- Unknown required capabilities or permission scopes cause rejection.
- Plugin outputs are untrusted artifacts and are validated before they enter a
  project workspace.

## Lifecycle

`DISCOVERED -> VERIFIED -> APPROVED -> INSTALLED -> ACTIVE`

A failed verification, revoked signature, incompatible platform API, or
administrator revocation transitions the plugin to `QUARANTINED` or
`REVOKED`. Installation never implies activation.
