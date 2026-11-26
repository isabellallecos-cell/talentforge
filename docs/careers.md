# Careers site and applications

Approved requisitions are published as **job postings** on a public careers
site. Anyone can read open postings and apply without an account. The demo only
uses synthetic jobs and fictional applicants; the site says so on every page.

```text
job_requisitions (approved) ──publish──▶ job_postings (open) ──close──▶ closed
applicants (one per organization + email) ──apply──▶ applications (submitted)
```

## Data (Flyway V4)

| Table | Notes |
|---|---|
| `job_postings` | One per requisition (`UNIQUE (requisition_id)`), same-organization FK to the requisition. Title, department, location, type, salary and description are **copied at publish time**, so later requisition edits do not change what applicants saw. `status` is `open` or `closed`; a closed posting must record who closed it and when. Optimistic `version`. |
| `applicants` | Per organization: `UNIQUE (organization_id, email)`, email stored lower-cased and trimmed (CHECK). The same person applying to two organizations is two unrelated rows. |
| `applications` | `UNIQUE (job_posting_id, applicant_id)`; composite FKs keep the posting and the applicant in the application's organization. `status` is only `submitted` until the stage state machine arrives; `consented_at` is required. |

Deleting an organization or requisition cascades to its postings and
applications. Rollback is a new forward migration (see the file header).

## API

Recruiter side (signed in, see [role-permissions.md](security/role-permissions.md)):

| Endpoint | Needs | Result |
|---|---|---|
| `GET /v1/organizations/{id}/jobs[?requisitionId=]` | `requisitions:view` | Postings with `applicationCount` and `publicPath` |
| `POST /v1/organizations/{id}/jobs` `{requisitionId}` | `jobs:publish` | 201; 409 `requisition_not_approved` or `already_published` |
| `POST /v1/organizations/{id}/jobs/{jobId}/close` `{version}` | `jobs:publish` | 200; 409 `stale_version` or `posting_closed` |

Public side (no session):

| Endpoint | Result |
|---|---|
| `GET /v1/public/organizations/{slug}/jobs` | The organization and its open postings, newest first |
| `GET /v1/public/organizations/{slug}/jobs/{jobId}` | One open posting |
| `POST /v1/public/organizations/{slug}/jobs/{jobId}/applications` | 202 `{status: "received", jobTitle, organizationName}` |

Public responses never include headcount, the hiring manager, the requisition
ID or who published the posting. An unknown organization, an unknown posting, a
posting of another organization and a closed posting all answer the same 404.

### Applying

Body: `fullName` (≤120, no control characters), `email` (≤254), optional
`phone` (≤40, digits and `+ ( ) - .`), optional `coverLetter` (≤4000) and
`privacyConsent`, which must be `true`. Problems come back as 422
`validation_failed` with every field listed.

- **Repeats are safe.** Applicant and application are written with
  `INSERT … ON CONFLICT DO NOTHING`, so retries and concurrent first
  submissions create one applicant and one application.
- **No overwrites, no enumeration.** A second submission with a known email
  does not change the stored applicant and gets the same 202 receipt as a
  first one, so the endpoint does not reveal who has applied.
- **Closing races.** The posting row is read with a shared lock while applying;
  a concurrent close waits, and an application that starts after the close
  gets 404.
- **CSRF** still applies: the web app fetches `GET /v1/auth/csrf` first.
- **Logs** carry posting, organization, applicant and application IDs only —
  never names, emails, phone numbers or cover letters.

## Web app

- `/careers/:slug` lists open jobs; `/careers/:slug/jobs/:jobId` shows one job
  and the application form, outside the signed-in shell. The form checks the
  API's rules first, maps a 422 back onto the fields, and replaces itself with
  a confirmation that receives focus.
- On an approved requisition (`/requisitions/:id`) the **Careers site** panel
  shows whether it is published, links to the public page, shows the number of
  applications, and offers **Publish** / **Close posting** to `jobs:publish`.

## Verified

- 98 JUnit tests (`JobPostingControllerTest`, `CareersControllerTest`, plus the
  existing suites).
- Against PostgreSQL 17: Flyway V1–V4 and Hibernate validation, 30 HTTP and
  database checks (publish rules, anonymous listing, repeat and 8 concurrent
  applications → one row each, closed postings, CHECK/unique/FK constraints,
  organization cascade), and no applicant data in the API log.
- 22 browser checks of the publish → apply → close flow.

## Not yet

- Résumé upload (next), rate limiting of anonymous applications, and the
  applicant-facing status page.
- Recruiters cannot list applications yet; that comes with the pipeline.
