import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { OrganizationContext } from './organization-context.service';
import { PermissionKey } from './navigation';

/**
 * Hides screens the member cannot use. The API enforces the same rule; this
 * only avoids showing a page that would fail on its first request.
 */
export function permissionGuard(permission: PermissionKey): CanActivateFn {
  return async () => {
    const organizations = inject(OrganizationContext);
    const router = inject(Router);
    const access = await organizations.ready();
    return access?.permissions.includes(permission) ? true : router.createUrlTree(['/']);
  };
}
