import { HttpClient } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { AuthService } from '../auth/auth.service';
import { Role } from '../auth/auth.models';
import { NAV_ITEMS, pickOrganization, visibleNavItems } from './navigation';

export interface OrganizationAccess {
  organizationId: string;
  role: Role;
  permissions: string[];
}

const STORAGE_KEY = 'tf.activeOrganization';

/**
 * The organization the user is working in, plus their permissions there
 * (from `GET /v1/organizations/{id}/access`). Permissions are re-read whenever
 * the organization changes, so a role change shows up on the next switch.
 */
@Injectable({ providedIn: 'root' })
export class OrganizationContext {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly selectedId = signal<string | null>(readRemembered());
  private readonly accessState = signal<OrganizationAccess | null>(null);
  private readonly failed = signal(false);
  private readonly inFlight = new Map<string, Promise<OrganizationAccess | null>>();

  readonly memberships = computed(() => this.auth.user()?.memberships ?? []);
  readonly activeId = computed(() => pickOrganization(this.memberships(), this.selectedId()));
  readonly active = computed(() => this.memberships().find((m) => m.organizationId === this.activeId()) ?? null);
  /** Access for the active organization only; stale answers are ignored. */
  readonly access = computed(() => {
    const access = this.accessState();
    return access && access.organizationId === this.activeId() ? access : null;
  });
  readonly accessFailed = this.failed.asReadonly();
  readonly permissions = computed(() => new Set(this.access()?.permissions ?? []));
  readonly navItems = computed(() => visibleNavItems(NAV_ITEMS, this.permissions()));

  constructor() {
    effect(() => {
      const id = this.activeId();
      if (id) {
        void this.loadAccess(id);
      }
    });
  }

  select(organizationId: string): void {
    this.selectedId.set(organizationId);
    try {
      localStorage.setItem(STORAGE_KEY, organizationId);
    } catch {
      // Storage disabled: the choice lasts for this page only.
    }
  }

  has(permission: string): boolean {
    return this.permissions().has(permission);
  }

  /** Resolves once access for the active organization is known (or failed). */
  async ready(): Promise<OrganizationAccess | null> {
    const id = this.activeId();
    if (!id) {
      return null;
    }
    return this.access() ?? this.loadAccess(id);
  }

  private loadAccess(organizationId: string): Promise<OrganizationAccess | null> {
    let pending = this.inFlight.get(organizationId);
    if (!pending) {
      pending = this.fetchAccess(organizationId).finally(() => this.inFlight.delete(organizationId));
      this.inFlight.set(organizationId, pending);
    }
    return pending;
  }

  private async fetchAccess(organizationId: string): Promise<OrganizationAccess | null> {
    try {
      const access = await firstValueFrom(
        this.http.get<OrganizationAccess>(`/v1/organizations/${encodeURIComponent(organizationId)}/access`),
      );
      this.failed.set(false);
      this.accessState.set(access);
      return access;
    } catch {
      if (this.activeId() === organizationId) {
        this.failed.set(true);
      }
      return null;
    }
  }
}

function readRemembered(): string | null {
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}
