# Standardized Progress Assessment

- Assessment date: 2026-11-23
- Assessed commit: `211a342ff0441872d7e958977825431082dd86c0` on `main`
- Status: reassessed after JWT-secured Spring generation and OpenAPI 3.x schema preview import; category ratings are reviewer scores against the same six-dimension rubric, not raw GitHub counters.
- Overall weighted score: **90.45%**

## Scoring rubric

| Dimension | Weight | Score | Weighted contribution |
|---|---:|---:|---:|
| Product capabilities | 40% | 88% | 35.20 |
| Tests and code quality | 20% | 95% | 19.00 |
| CI/CD and build reliability | 15% | 100% | 15.00 |
| Security and access control | 10% | 85% | 8.50 |
| Documentation and operability | 10% | 95% | 9.50 |
| Deployment and release readiness | 5% | 65% | 3.25 |
| **Total** | **100%** | | **90.45%** |

Formula: overall progress = sum(category score × category weight). A category score is based on implemented scope and its verification evidence. Documentation alone does not count as implementation. A green workflow proves only the checks included in that workflow; it does not prove production readiness.

## Evidence checked

README and roadmap show Phase 0–2 complete, partial Phase 3–5 and incomplete Phase 6. The latest verified scope includes tenant-scoped project access, deterministic Java/Spring/PostgreSQL generation and golden fixtures, JWT resource-server support in generated applications, and preview-only OpenAPI 3.0/3.1 schema import. Remote references are not fetched and schema composition/API path import remain explicit limitations.

- Exact assessed main commit: `211a342ff0441872d7e958977825431082dd86c0`.
- Latest main CI/build evidence: [run 45126837021](https://github.com/alighaziam/devforge-platform/actions/runs/45126837021) — passed.
- Latest main CodeQL/security evidence: [run 45126836974](https://github.com/alighaziam/devforge-platform/actions/runs/45126836974) — passed.
- PR #22 (JWT-secured Spring generation), PR #23 (OpenAPI 3.x schema preview import), and PR #24 (assessment record) are merged.

## Remaining release gates

Golden-file fixtures and regeneration safety; isolated build workers and complete generated-project validation evidence; plugin signing/verification/sandbox execution; additional stack generators; Git/PR delivery, CI/CD deployment profiles and SBOM. Latest Platform CI success located on 2026-11-18 commit 22f90076; CodeQL passed 2026-11-19.

## Rules for future updates

1. Recalculate the same six dimensions with the same weights; do not change the denominator between reports.
2. Mark a capability complete only when implementation exists and the relevant automated test or reproducible manual check passes.
3. Treat missing, skipped, or unavailable checks as unverified, never as passed.
4. Keep production-only integrations, external services, deployment validation and manual QA separate from local mocks/sandboxes.
5. Record the assessed commit SHA and link the exact workflow run used as evidence.
6. Update this file when evidence changes; do not infer progress from commit count.

This is a consistent engineering assessment baseline, not a claim that a single objectively exact percentage exists independent of the scoring rubric.


## PR #18 CI correction — 20 November 2026

- Platform CI exposed that Maven's default resource exclusions omit dot-prefixed files such as `.gitignore`; the golden test therefore could not load that fixture from the test classpath.
- Renamed the fixture to `gitignore.golden` and updated the test to load the resource-safe name. The hidden `.gitignore` fixture was removed.
- The corrected branch has been pushed; CI must rerun on the new head SHA before this increment is considered verified or merged.
- No score increase is recorded until the corrected Platform CI passes.

## Latest merged increment — 19 November 2026\n\nRead-only deterministic domain-model diff endpoint merged in PR #17; Platform CI and CodeQL passed. OpenAPI/XML/PDF/DOCX/spreadsheet import and sandbox/delivery phases remain incomplete.\n

## Latest merged increment — 20 November 2026

- PR #18 merged as `0a74522255973935fda4f91cdbaff9fb24793b87` after Platform CI and CodeQL both passed on head `a88c278b8f01abe8c936e9531567dd25e9e4799d`.
- Golden fixtures now cover README, environment example, gitignore, Dockerfile and Docker Compose output. The `.gitignore` fixture is stored as `gitignore.golden` because Maven excludes dot-prefixed resource files by default.
- Java source and Flyway migration golden fixtures remain open; no overall score increase is claimed without a full reassessment.


## Java source and Flyway golden fixtures — 20 November 2026

- Added byte-for-byte golden fixtures for the generated `Customer.java` entity and `V1__CreateDomainSchema.java` Flyway migration, based on the generator's current deterministic output for the existing Orders test specification.
- Replaced substring-only checks for those two outputs with exact fixture comparisons in `JavaSpringPostgresGeneratorTests`.
- Fixture resource names use the `.golden` suffix so Maven does not treat generated Java samples as compilable test source.
- PR #19 merged as `a81dd987de3d454661c035b325c75a81394c9f13` after Platform CI (run 44676958714) and CodeQL (run 44676958558) passed on head `b668bc345dd4ec27ca879374701900253fa5e75a`.
- Golden fixtures now cover generated `Customer.java` and `V1__CreateDomainSchema.java`, with byte-for-byte equality assertions.
- No overall score increase is claimed; the six rubric dimensions have not been fully reassessed.


## Assessment note — 20 November 2026

- PR #19 adds byte-for-byte golden fixtures for generated Java entity and Flyway migration output; the exact-head Platform CI and CodeQL runs passed before merge.
- The prior 70.5% weighted baseline is retained as the last full six-dimension assessment. This increment closes a specific Phase 4 gate but does not by itself justify a new overall score without reassessing all six rubric dimensions.

## Daily repository audit — 20 November 2026

- Audited default branch main at 81a7a05e9f7df9dea53f145c793b94cdd4ed5e91.
- Latest Platform CI: [run 44717475965](https://github.com/alighaziam/devforge-platform/actions/runs/44717475965) — passed.
- Latest CodeQL: [run 44717475964](https://github.com/alighaziam/devforge-platform/actions/runs/44717475964) — passed.
- No open pull requests were found. PR #19 (byte-for-byte Java entity and Flyway migration golden fixtures) is merged and verified.
- Earlier model-diff, plugin, tenant-isolation and generator work remains in main; branches that are only behind main are not treated as unfinished work.
- No progress percentage is changed. Additional import formats, broader technology generators, isolated execution sandbox, plugin delivery and Git/CI project delivery remain open roadmap gates.
- The hourly PR Guardian source now uses explicit repository context and clean/check/review gates; scheduled post-update confirmation remains in progress.


## Latest verified increment — 22 November 2026

- The Java/Spring/PostgreSQL generator now emits entity-specific REST controllers with GET list, GET by UUID, POST, PUT and DELETE endpoints. Update endpoints copy only declared non-ID attributes and relationships onto a loaded entity rather than replacing the identifier from request data.
- Generator tests assert route uniqueness, all CRUD mappings, not-found behavior, and generated update mapping.
- Platform CI passed on assessed commit `3cf118a602c77ff1d601e759f2241da7fff0e5f5` ([run 44997188126](https://github.com/alighaziam/devforge-platform/actions/runs/44997188126)); CodeQL passed ([run 44997188107](https://github.com/alighaziam/devforge-platform/actions/runs/44997188107)).
- Remaining work includes additional stack generators, sandboxed third-party plugins, expanded import formats, Git/PR delivery, deployment profiles and SBOM generation. Generated CRUD APIs remain unauthenticated when the selected specification explicitly chooses no authentication and must be secured before production use.


## Verified checkpoint — 2026-11-23 (OpenAPI model import)

- PR #22 added JWT resource-server support to generated Spring applications, with fail-closed security when authentication is enabled.
- PR #23 added OpenAPI 3.0/3.1 JSON schema preview import from `components.schemas`: primitive attributes, local object references, one-to-many arrays, nullable type unions, and explicit limitations.
- Remote references are never fetched; schema composition and API path/security-scheme import remain explicit limitations. Import is preview-only and retains the 512 KiB input limit.
- Assessed main commit: `211a342ff0441872d7e958977825431082dd86c0`.
- Main CI [run 45126837021](https://github.com/alighaziam/devforge-platform/actions/runs/45126837021) passed Java 25 platform tests/build and Angular Studio build.
- CodeQL [run 45126836974](https://github.com/alighaziam/devforge-platform/actions/runs/45126836974) passed actions, Java/Kotlin and JavaScript/TypeScript analysis.
- The 90.45% score uses the six-dimension weights above. Remaining gaps include full model mapping/merge, sandboxed generated-app validation, plugin execution, and automated delivery.
