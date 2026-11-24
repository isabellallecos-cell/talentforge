import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from '../auth/auth.service';

const AUTH_ENDPOINTS = ['/v1/auth/me', '/v1/auth/login', '/v1/auth/logout', '/v1/auth/csrf'];

/** A 401 from any other endpoint means the session ended; go back to sign-in. */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !AUTH_ENDPOINTS.includes(req.url)) {
        auth.clear();
        const returnUrl = router.url;
        void router.navigate(['/login'], {
          queryParams: returnUrl && returnUrl !== '/' ? { returnUrl, expired: 1 } : { expired: 1 },
        });
      }
      return throwError(() => error);
    }),
  );
};
