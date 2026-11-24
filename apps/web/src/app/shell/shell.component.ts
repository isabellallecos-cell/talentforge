import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ROLE_LABELS } from '../core/auth/auth.models';
import { AuthService } from '../core/auth/auth.service';
import { OrganizationContext } from '../core/organization/organization-context.service';

@Component({
  selector: 'tf-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.css',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly organizations = inject(OrganizationContext);

  readonly user = this.auth.user;
  readonly roleLabel = computed(() => {
    const active = this.organizations.active();
    return active ? ROLE_LABELS[active.role] : null;
  });
  readonly signingOut = signal(false);

  switchOrganization(event: Event): void {
    const id = (event.target as HTMLSelectElement).value;
    this.organizations.select(id);
    // Screens are organization-scoped; start again from the overview.
    void this.router.navigateByUrl('/');
  }

  async signOut(): Promise<void> {
    this.signingOut.set(true);
    try {
      await this.auth.logout();
    } catch {
      // The local session is cleared either way; the server session expires on its own.
    } finally {
      this.signingOut.set(false);
      await this.router.navigateByUrl('/login');
    }
  }
}
