# 0001. Angular/Spring workspace and synthetic hiring data strategy

- **Status:** Accepted
- **Date:** 2026-11-17

## Context

TalentForge manages job requisitions, applications, interviews, scorecards and
AI-drafted summaries for a hiring team. The signature demo publishes a synthetic
job, takes a fictional application through the hiring stages, schedules an
interview, records a scorecard, generates a source-linked summary, lets a person
decide the next stage and exports the audit history.

The demo must run offline on one machine, be repeatable from scratch and use only
synthetic data. Two early choices shape everything after them:

1. How the repository and runtime are laid out, and which component owns which state.
2. Where the hiring data used in development, tests and the demo comes from.

## Decision

### Workspace layout

One repository, built as a modular monolith with a separate worker:

| Path | Contents |
|---|---|
| `apps/web/` | Angular application (TypeScript, standalone components, Angular CLI). |
| `services/api/` | Spring Boot API with Spring Security. The only writer of domain data. |
| `services/worker/` | Spring Boot application for slow jobs: resume parsing, AI summary drafts, retention. |
| `packages/contracts/` | OpenAPI documents for `/v1`; the Angular client is generated from them. |
| `packages/fixtures/` | Synthetic hiring data generator and the generated sample sets. |
| `infra/compose/` | Docker Compose stack for local services. |
| `tests/` | Integration and end-to-end tests. |
| `docs/` | ADRs (`docs/adr/`) and weekly progress notes (`docs/progress/`). |

Both Spring Boot applications are Gradle subprojects of one build (Java 21) so they
share the domain module and dependency versions. The Angular app is built with npm.

Local services run through Docker Compose with pinned image tags:

| Service | Role |
|---|---|
| PostgreSQL + pgvector | Single source of truth: organizations, memberships, jobs, applications, stage transitions, interviews, scorecards, AI drafts and audit events. pgvector stores note embeddings for summary retrieval. Schema changes go through Flyway migrations owned by `services/api`. |
| Redis | Job queue between API and worker, plus short-lived caches. Never authoritative; losing Redis must not lose data. |

Ownership rules:

- **The API owns every write that changes hiring state.** The worker stores its
  results (parsed resume fields, AI drafts) as pending records; only an authorized
  person acting through the API applies them.
- **Every stage change is a row in `stage_transitions`** with actor, reason and the
  application version it was based on. Concurrent changes are rejected with `409`
  through optimistic version checks.
- **Every query is scoped to an organization** taken from the authenticated
  membership, never from the request body.
- **The AI adapter is an interface with a deterministic mock.** CI and the demo use
  the mock; a real model is optional configuration.

### Synthetic hiring data

- **Generated, not hand-written.** A Python generator in `packages/fixtures`
  produces the data from a fixed seed and reference date. The same seed always
  produces byte-identical output, so tests can assert exact values.
- **Versioned.** Each set carries a `fixture_version`; seeds loaded into the
  database and test snapshots record the version they came from.
- **Plainly fictional.** Organizations, people, schools and employers are invented
  from small word lists. Email addresses use the reserved `example.com` and
  `example.org` domains; phone numbers use the fictional 555-01xx range. No real
  resumes, names of real people or real companies.
- **No protected traits.** Applicant records contain no age, gender, ethnicity,
  photo, nationality or similar fields, so nothing downstream can score on them.
- **Covers the workflow.** One set includes several organizations (for
  cross-tenant tests), users in every role, open and closed jobs, applications in
  every stage, interviews with overlapping slots across time zones (for conflict
  tests), scorecards and reviewer notes for summaries.
- **Edge cases on purpose.** The generator can add known problem records: an
  oversized resume, an unsupported file type, a duplicate application, a note
  containing prompt-injection text and a note from another organization. Each is
  listed in the fixture manifest so tests know which checks must reject it.

## Consequences

- The demo and CI can rebuild everything from nothing with Compose and a seed,
  and failures are reproducible.
- Splitting API and worker adds a queue hop, but keeps slow parsing and AI calls
  out of request handling, and keeps authorization and auditing in one place.
- The Java build, the Angular build and the Python generator mean three toolchains
  in CI; each gets its own job.
- The generator must change together with the schema; incompatible changes bump
  `fixture_version`.

## Alternatives considered

- **Microservices per domain area.** Rejected for now: transactions across
  applications, stage transitions and audit events are simpler in one database
  and one writer. Boundaries are kept as modules so they can be split later.
- **A separate vector database.** Rejected: pgvector keeps embeddings next to the
  notes they came from and under the same organization scoping.
- **Maven instead of Gradle.** Either works; Gradle's multi-project build fits the
  shared domain module with less configuration.
- **Public resume datasets.** Rejected: they contain or resemble real people,
  include protected traits and cannot produce controlled edge cases.
