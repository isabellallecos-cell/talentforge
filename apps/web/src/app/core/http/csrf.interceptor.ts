import { HttpClient, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { switchMap } from 'rxjs';

import { CsrfResponse } from '../auth/auth.models';

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS']);

/**
 * Angular's built-in XSRF support copies the XSRF-TOKEN cookie into the
 * header, but the server only issues that cookie on demand and rotates it at
 * sign-in. When a write goes out without the header, fetch a token first.
 */
export const csrfInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  if (SAFE_METHODS.has(req.method) || !req.url.startsWith('/v1/') || req.headers.has('X-XSRF-TOKEN')) {
    return next(req);
  }
  const http = inject(HttpClient);
  return http
    .get<CsrfResponse>('/v1/auth/csrf')
    .pipe(switchMap((csrf) => next(req.clone({ setHeaders: { [csrf.headerName]: csrf.token } }))));
};
