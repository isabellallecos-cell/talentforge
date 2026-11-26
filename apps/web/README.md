# TalentForge web

Angular 20 (standalone components, zoneless) client for the TalentForge API.

## Run locally

Requires Node.js 20.19+ and the API on `http://localhost:8080`
(`services/api`, e.g. with the `demo` profile and `TALENTFORGE_DEMO_PASSWORD` set).

```bash
npm ci
npm start        # http://localhost:4200, proxies /v1 to the API (proxy.conf.json)
npm test         # unit tests (Vitest)
npm run build
```

## How sign-in works

- The session is the API's HttpOnly `TF_SESSION` cookie; the app asks
  `GET /v1/auth/me` on start-up to find out who is signed in.
- Writes carry the `X-XSRF-TOKEN` header. The server rotates the CSRF token at
  sign-in, so `csrfInterceptor` fetches `GET /v1/auth/csrf` when a write would
  otherwise go out without one.
- A 401 from any non-auth endpoint clears the local user and returns to
  `/login?expired=1`, keeping the current page as `returnUrl`. Only in-app
  paths are accepted as `returnUrl` (`safeReturnUrl`).
- Navigation and route guards use `GET /v1/organizations/{id}/access`. Hiding a
  link is only a convenience: the API checks every request.

## Requisitions

Under **Requisitions** (`requisitions:view`):

- `/requisitions` lists them, newest change first; `?status=` filters
  (draft, pending approval, approved, rejected) and stays in the URL.
- `/requisitions/new` and `/requisitions/:id/edit` (`requisitions:create`)
  create and edit drafts. The form checks the same rules as the API (required
  fields, 1–50 openings, salary range with a currency) and maps the API's 422
  field list back onto the fields. The hiring manager is picked from
  `GET …/requisitions/hiring-managers`.
- `/requisitions/:id` shows the requisition, its history, and one button per
  action in the response's `allowedActions`, so a submitter never sees
  Approve. Approve asks for an optional note, Reject for a required reason.
- Every write sends the `version` the page loaded. If someone changed the
  requisition in the meantime the API answers 409 and the page offers to
  reload instead of overwriting.

The workflow itself is described in `docs/requisitions.md`.

## Careers site

- `/careers/:slug` and `/careers/:slug/jobs/:jobId` are public (no sign-in,
  outside the shell): open jobs, one job, and the application form. The form
  mirrors the API's rules, maps its 422 field list back, and shows a
  confirmation that receives focus. Try `/careers/northwind-talent` with the
  `demo` profile once a job is published.
- Approved requisitions show a **Careers site** panel: publish, view the public
  page, application count, close (`jobs:publish`).

See `docs/careers.md`.

## Demo accounts

With the `demo` profile the API creates one synthetic organization with a user
per role (`admin@`, `recruiter@`, `manager@`, `interviewer@northwind.example.org`),
all using `TALENTFORGE_DEMO_PASSWORD`.
