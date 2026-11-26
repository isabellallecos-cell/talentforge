# Job requisitions

A requisition is a request to open a position. It has to be approved before it
can be published to applicants as a job posting (see [careers.md](careers.md)).

## Lifecycle

```text
draft ──submit──▶ pending_approval ──approve──▶ approved
  ▲  ▲               │      │
  │  └───withdraw────┘      └──reject──▶ rejected
  └────────────────reopen──────────────────┘
```

| Action | From | To | Who |
|---|---|---|---|
| create | – | draft | `requisitions:create` (admin, recruiter, hiring manager) |
| update | draft | draft | `requisitions:create` |
| submit | draft | pending_approval | `requisitions:create` |
| withdraw | pending_approval | draft | the person who submitted it, or an admin |
| approve | pending_approval | approved | the assigned hiring manager or an admin, **not** the submitter |
| reject | pending_approval | rejected | same as approve; a reason (`note`) is required |
| reopen | rejected | draft | `requisitions:create` |

Approved is final; publishing and closing the job posting are separate
(see [careers.md](careers.md)). The rules live in `RequisitionWorkflow` (no I/O,
covered by `RequisitionWorkflowTest`); the database repeats the important ones
as CHECK constraints (status values, four-eyes, rejection reason, decision
fields, salary range).

## API

All routes are under `/v1/organizations/{organizationId}/requisitions`. Every
response carries `version` and `allowedActions` (what *this* caller can do
now), so the web app shows only the buttons that will work.

| Request | Result |
|---|---|
| `GET ?status=pending_approval` | list, newest change first |
| `POST` with the fields below | 201, a draft |
| `PUT /{id}` with the fields and `version` | the edited draft |
| `POST /{id}/{action}` with `{"version": n, "note": "…"}` | the requisition after the change |
| `GET /{id}/events` | the history, oldest first |
| `GET /hiring-managers` | members who can be named hiring manager |

Fields: `title`, `department`, `location` (required, trimmed),
`employmentType` (`full_time`, `part_time`, `contract`, `internship`),
`headcount` (1–50), optional `salaryMin`/`salaryMax` (whole units per year) with
`salaryCurrency` (three letters, required when a salary is given),
`description` (≤ 4000), `hiringManagerId` (a hiring manager or admin of the
organization).

| Error | Status | Code |
|---|---|---|
| not a member / unknown requisition or action | 404 | `not_found` |
| role lacks the permission | 403 | `forbidden` |
| row-level rule (own submission, not the assigned manager) | 403 | `not_allowed`, with the reason |
| invalid field, missing `version`, reject without `note` | 422 | `validation_failed` |
| action not possible from the current status | 409 | `invalid_transition` |
| `version` is not the stored one (someone else changed it) | 409 | `stale_version` |
| unknown `?status=` | 400 | `invalid_parameter` |

## History and logging

Every change writes one row to `requisition_events` (actor, action, from/to
status, note) in the same transaction. The table is append-only: a trigger
rejects UPDATE and direct DELETE; rows only go when their organization is
deleted. Logs carry requisition, user and organization IDs and the action,
never titles, notes or emails.

## Known limits

- A hiring manager's membership cannot be removed while a requisition names
  them (foreign key). Reassigning requisitions has to come with member
  management.
- The list is not paginated; fine for demo organizations, not for large ones.
