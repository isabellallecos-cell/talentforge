import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Sends visitors without a session to the sign-in page, remembering where they were going. */
export const authGuard: CanActivateFn = async (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const user = await auth.restoreSession();
  if (user) {
    return true;
  }
  return router.createUrlTree(['/login'], {
    queryParams: state.url && state.url !== '/' ? { returnUrl: state.url } : {},
  });
};

/** Keeps signed-in users away from the sign-in page. */
export const guestGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try {
    return (await auth.restoreSession()) ? router.createUrlTree(['/']) : true;
  } catch {
    // API unreachable: still show the form, which reports the problem on submit.
    return true;
  }
};
