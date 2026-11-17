# Week 01 acceptance cases: workspace and synthetic hiring data

Scope: the repository layout, local services and the synthetic hiring data
generator described in
[ADR 0001](../adr/0001-angular-spring-workspace-and-synthetic-data.md).
Each case is written as Given / When / Then and must be verifiable from a clean
checkout.

## Workspace and tooling

**AC-01.1 — Clean build**
- Given a fresh clone, JDK 21, the documented Node.js version and Python 3
- When the documented build commands are run (Gradle for `services/`, npm for
  `apps/web`)
- Then the API, the worker and the Angular app compile without errors

**AC-01.2 — Uniform checks**
- Given the built workspace
- When lint, typecheck and tests are run for each toolchain
- Then each part runs its own checks, and a failure in any of them fails the
  overall run

**AC-01.3 — Local services**
- Given Docker is available
- When the Compose stack in `infra/compose` is started
- Then PostgreSQL (with the `vector` extension available) and Redis report
  healthy, image tags are pinned, and no service needs credentials that are not
  in `.env.example`

**AC-01.4 — API health**
- Given the Compose stack is running
- When the API starts against it
- Then its health endpoint reports `UP` and the startup log contains no
  passwords or connection secrets

**AC-01.5 — No secrets committed**
- Given the repository history
- When a secret scanner runs in CI
- Then no keys, tokens or passwords are found, and `.env` files are ignored by git

## Synthetic hiring data generator

**AC-01.6 — Deterministic output**
- Given the default seed and reference date
- When the generator runs twice
- Then both runs produce byte-identical files

**AC-01.7 — Different seed, different data**
- Given two different seeds
- When the generator runs with each
- Then the outputs differ, and both pass the same validation checks

**AC-01.8 — Manifest**
- Given any generated set
- When its manifest is read
- Then it states the `fixture_version`, seed, reference date, record counts per
  entity and the list of deliberate edge-case records

**AC-01.9 — Tenant isolation coverage**
- Given the default set
- When records are grouped by organization
- Then there are at least two organizations, and every membership, job,
  application, interview, scorecard and note belongs to exactly one of them

**AC-01.10 — Referential integrity**
- Given the default set
- When references are checked
- Then every application points to a job in the same organization, every
  interview and scorecard points to an application in the same organization, and
  every interviewer is a member of that organization (the deliberate cross-tenant
  note in AC-01.15 is the only listed exception)

**AC-01.11 — Every role and stage covered**
- Given the default set
- Then each organization has at least one recruiter, hiring manager, interviewer
  and admin; there are open and closed jobs; and applications exist in every
  hiring stage

**AC-01.12 — Consistent stage history**
- Given an application's stage transitions in time order
- Then each transition starts from the stage the previous one ended in, has an
  actor and a reason, and the last one matches the application's current stage

**AC-01.13 — Scheduling conflicts present**
- Given the default set
- Then at least one interviewer has two overlapping interviews, stored in
  different time zones, so conflict detection can be tested; all times are
  stored with an explicit offset

**AC-01.14 — Plainly fictional, no protected traits**
- Given any generated person, organization, school or employer
- Then names come from the generator's word lists, emails use `example.com` or
  `example.org`, phone numbers use the 555-01xx range, and applicant records have
  no age, birth date, gender, ethnicity, nationality or photo fields

**AC-01.15 — Edge cases listed and present**
- Given the generator is run with edge cases enabled
- Then the set contains an oversized resume, an unsupported file type, a
  duplicate application, a note containing prompt-injection text and a note that
  belongs to another organization, and each one is listed in the manifest with
  the check that must reject it

## Out of scope this week

- Authentication and roles in the API (week 2)
- Loading fixtures through the API (they are loaded directly for now)
- Resume file contents beyond metadata (week 3)
- Performance-sized datasets

## Risks and open questions

- Three toolchains (Java, Node.js, Python) make local setup heavier; Compose and
  documented versions should keep it predictable.
- The fixture schema will change with every domain migration from week 2 on; the
  generator has to change in the same branch and bump `fixture_version` when
  the change is incompatible.
- Prompt-injection examples must be realistic enough to test the summary
  pipeline without containing anything harmful if shown in the UI.
- Time zone handling in overlapping interviews is easy to get subtly wrong;
  generated times stay in UTC with explicit offsets for display.
