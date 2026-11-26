# Roles and permissions

Roles are held per organization (`memberships.role`). A user can be a recruiter
in one organization and an interviewer in another. The matrix lives in
`services/api/.../access/RolePermissions.java` and `RolePermissionsTest` pins it.

| Permission | Key | admin | recruiter | hiring_manager | interviewer |
|---|---|:-:|:-:|:-:|:-:|
| See organization members | `members:view` | ✅ | ✅ | | |
| Change members and roles | `members:manage` | ✅ | | | |
| See job requisitions | `requisitions:view` | ✅ | ✅ | ✅ | |
| Draft job requisitions | `requisitions:create` | ✅ | ✅ | ✅ | |
| Approve or reject requisitions | `requisitions:approve` | ✅ | | ✅ | |
| Publish and close job postings | `jobs:publish` | ✅ | ✅ | | |
| See candidates and applications | `candidates:view` | ✅ | ✅ | ✅ | |
| Add candidates, move stages | `candidates:manage` | ✅ | ✅ | | |
| See interviews assigned to me | `interviews:view-assigned` | ✅ | ✅ | ✅ | ✅ |
| Submit my own scorecards | `scorecards:submit` | ✅ | | ✅ | ✅ |

## How a request is checked

1. Spring Security requires a signed-in session for every `/v1/**` route except
   `GET /v1/auth/csrf`, `POST /v1/auth/login` and the public careers routes
   under `/v1/public/organizations/{slug}/jobs` (401 `unauthenticated`).
2. `OrganizationAccess` reads the caller's membership for the organization in
   the URL **on every request**. Roles are not cached in the session, so a role
   change or removal applies on the next request.
3. Not a member, or the organization does not exist → **404 `not_found`**. The
   two cases look the same so organization IDs cannot be probed.
4. Member without the permission → **403 `forbidden`**, the same body Spring
   Security uses for CSRF failures.
5. Denials are logged at INFO with user ID, organization ID, role and permission
   key only. Emails and names are never logged.

## Endpoints so far

| Endpoint | Needs |
|---|---|
| `GET /v1/organizations/{id}/access` | membership; returns `role` and sorted `permissions` keys for the web app's navigation |
| `GET /v1/organizations/{id}/members` | `members:view` |
| `GET /v1/organizations/{id}/requisitions[?status=]`, `GET …/requisitions/{rid}`, `GET …/{rid}/events` | `requisitions:view` |
| `POST …/requisitions`, `PUT …/requisitions/{rid}`, `GET …/requisitions/hiring-managers` | `requisitions:create` |
| `POST …/requisitions/{rid}/submit`, `…/withdraw`, `…/reopen` | `requisitions:create` (withdraw: the submitter or an admin) |
| `POST …/requisitions/{rid}/approve`, `…/reject` | `requisitions:approve`, plus the row-level rules in [requisitions.md](../requisitions.md) |
| `GET /v1/organizations/{id}/jobs` | `requisitions:view` |
| `POST /v1/organizations/{id}/jobs`, `POST …/jobs/{jid}/close` | `jobs:publish`; only approved requisitions, once each ([careers.md](../careers.md)) |
| `GET /v1/public/organizations/{slug}/jobs[/{jid}]`, `POST …/{jid}/applications` | nothing (public); open postings only, CSRF still required for the POST |

The web app may hide screens using `permissions`, but the API never trusts the
client: every endpoint repeats the check.

## Open risks

- Scoping inside an organization needs row-level checks in each feature, not
  just this matrix. Requisitions have them (only the assigned hiring manager or
  an admin decides, never the submitter); interviews still need "only my own".
- `members:manage` has no endpoint yet; the last-admin rule (an organization
  must keep one admin) has to be enforced when it is added.
