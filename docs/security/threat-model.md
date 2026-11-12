# Security Threat Model (Initial)

## Assets

- Source requirements and uploaded documents.
- Project specifications, domain models, and generated source.
- User credentials, provider tokens, deployment credentials, and secrets.
- Plugin packages, generator outputs, build artifacts, and SBOMs.
- Tenant metadata, audit trails, and validation evidence.

## Threat actors and trust assumptions

Assume malicious or compromised users, malicious uploaded documents, prompt-injection content, compromised plugins/dependencies, malicious generated build scripts, compromised external providers, and accidental operator misuse. Public repository visibility means every committed file is public.

## Initial threat register

| Threat | Required controls |
|---|---|
| Prompt injection in uploaded artifacts | Treat content as data; isolate model instructions; never let extracted text grant tool permissions |
| Path traversal / archive bombs | Canonical path checks, archive entry/size limits, symlink rejection |
| Malicious generator or build script | Ephemeral sandbox, non-root, no host mounts, CPU/memory/time quotas, network deny-by-default |
| Secret leakage | Secret manager, short-lived credentials, redaction, no secrets in generated files or logs |
| Cross-tenant access | Tenant-scoped authorization at service and data layers; isolation tests |
| Dependency/supply-chain compromise | Pin versions, verify checksums/signatures, SBOM, vulnerability and license scanning |
| Unsafe Git operations | Least-privilege OAuth scopes, explicit repository selection, branch/PR workflow, no force push by default |
| Generated insecure code | Secure templates, static analysis, dependency scanning, security tests, explicit findings |
| Public repository disclosure | Never commit credentials, private customer data, production config, or real tokens |

## Sandbox baseline

The future execution service must use ephemeral isolated workers with non-root identity, read-only base image, bounded writable workspace, process/network controls, resource quotas, execution timeout, artifact scanning, and destruction after completion. Do not execute generated code in the platform API process.

## Public repository policy

This repository is public by user decision. Commit only source, documentation, schemas, and safe examples. Use placeholders for secrets and local configuration. Review every generated file before commit.

## Security standards

Use OWASP ASVS 5.0.0 as a security verification reference, plus applicable OWASP API Security guidance. Map controls to testable requirements as the security module matures.

## Status

This is an initial threat model, not a completed security assessment. Threats and controls must be expanded before exposing generation or execution endpoints to untrusted users.
