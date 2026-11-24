import { Component, inject } from '@angular/core';

import { ROLE_LABELS } from '../../core/auth/auth.models';
import { AuthService } from '../../core/auth/auth.service';
import { OrganizationContext } from '../../core/organization/organization-context.service';

@Component({
  selector: 'tf-home',
  template: `
    @if (organizations.active(); as active) {
      <h1>{{ active.organizationName }}</h1>
      <p class="muted">
        Signed in as {{ user()?.displayName }} · {{ roles[active.role] }}
      </p>

      <section aria-labelledby="access-heading">
        <h2 id="access-heading">What you can do here</h2>
        @if (organizations.access(); as access) {
          <ul class="permissions">
            @for (permission of access.permissions; track permission) {
              <li><code>{{ permission }}</code></li>
            } @empty {
              <li>No permissions in this organization.</li>
            }
          </ul>
        } @else if (!organizations.accessFailed()) {
          <p class="muted">Loading permissions…</p>
        }
      </section>
    }
  `,
  styles: `
    h1 {
      margin: 0 0 0.25rem;
    }
    .muted {
      color: var(--tf-muted);
    }
    .permissions {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      padding: 0;
      list-style: none;
    }
    .permissions li {
      padding: 0.25rem 0.5rem;
      border: 1px solid var(--tf-border);
      border-radius: 0.375rem;
      background: var(--tf-surface);
    }
  `,
})
export class HomeComponent {
  readonly organizations = inject(OrganizationContext);
  readonly user = inject(AuthService).user;
  readonly roles = ROLE_LABELS;
}
