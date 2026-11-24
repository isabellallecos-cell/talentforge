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
| See candidates and applications | `candidates:view` | ✅ | ✅ | ✅ | |
| Add candidates, move stages | `candidates:manage` | ✅ | ✅ | | |
| See interviews assigned to me | `interviews:view-assigned` | ✅ | ✅ | ✅ | ✅ |
| Submit my own scorecards | `scorecards:submit` | ✅ | | ✅ | ✅ |

## How a request is checked

1. Spring Security requires a signed-in session for every `/v1/**` route except
   `GET /v1/auth/csrf` and `POST /v1/auth/login` (401 `unauthenticated`).
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

The web app may hide screens using `permissions`, but the API never trusts the
client: every endpoint repeats the check.

## Open risks

- Scoping inside an organization (an interviewer seeing *only* their own
  interviews, a hiring manager approving only their own requisitions) needs
  row-level checks in those features, not just this matrix.
- `members:manage` has no endpoint yet; the last-admin rule (an organization
  must keep one admin) has to be enforced when it is added.
