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

## Demo accounts

With the `demo` profile the API creates one synthetic organization with a user
per role (`admin@`, `recruiter@`, `manager@`, `interviewer@northwind.example.org`),
all using `TALENTFORGE_DEMO_PASSWORD`.
