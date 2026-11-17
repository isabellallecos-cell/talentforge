# Week 01 progress — Workspace and synthetic hiring data

| Day | Task | Status | Evidence |
|---|---|---|---|
| D01 | Acceptance cases and ADR | Done | [ADR 0001](../adr/0001-angular-spring-workspace-and-synthetic-data.md), [acceptance cases](../acceptance/week-01-workspace-and-hiring-fixtures.md) |
| D02 | Core data schema and migration | Not started | |
| D03 | API endpoint and validation | Not started | |
| D04 | UI workflow with error states | Not started | |
| D05 | Unit and integration tests | Not started | |
| D06 | Observability and security checks | Not started | |
| D07 | Demo, regressions and next risks | Not started | |

## Notes

- ADR 0001 records the workspace layout (Angular app, Spring Boot API and
  worker, contracts, fixtures, Compose) and the synthetic data rules.
- Week 1 acceptance cases AC-01.1 to AC-01.15 written; none verified yet.
- Workspace scaffold added: Gradle build for `services/api` and `services/worker`,
  Angular app in `apps/web`, Compose stack in `infra/compose`.
- Synthetic hiring data generator added in `packages/fixtures` with tests for
  AC-01.6 to AC-01.15 (13 tests passing). Default set: 3 organizations, 46
  applications across every stage, 5 listed edge cases.
- Not yet verified: AC-01.1 to AC-01.5 (need JDK 21, Node.js and Docker).
