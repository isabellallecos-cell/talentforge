import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { toApiFailure } from '../../core/http/api-error';
import { OrganizationContext } from '../../core/organization/organization-context.service';
import {
  EMPLOYMENT_TYPE_LABELS,
  Requisition,
  STATUSES,
  STATUS_LABELS,
  parseStatusFilter,
} from '../../core/requisitions/requisition.models';
import { RequisitionsApi } from '../../core/requisitions/requisitions.service';

@Component({
  selector: 'tf-requisition-list',
  imports: [DatePipe, RouterLink],
  template: `
    <div class="page-header">
      <h1>Requisitions</h1>
      @if (organizations.has('requisitions:create')) {
        <a class="button" routerLink="/requisitions/new">New requisition</a>
      }
    </div>

    <label class="filter">
      Status
      <select (change)="changeFilter($event)">
        <option value="" [selected]="!filter()">All statuses</option>
        @for (status of statuses; track status) {
          <option [value]="status" [selected]="status === filter()">{{ statusLabels[status] }}</option>
        }
      </select>
    </label>

    @if (error(); as message) {
      <p class="error" role="alert">{{ message }}</p>
    } @else if (requisitions(); as requisitions) {
      @if (requisitions.length === 0) {
        <p class="muted">{{ emptyMessage() }}</p>
      } @else {
        <table>
          <caption class="visually-hidden">Requisitions, most recently changed first</caption>
          <thead>
            <tr>
              <th scope="col">Title</th>
              <th scope="col">Department</th>
              <th scope="col">Hiring manager</th>
              <th scope="col">Status</th>
              <th scope="col">Updated</th>
            </tr>
          </thead>
          <tbody>
            @for (r of requisitions; track r.id) {
              <tr>
                <td>
                  <a [routerLink]="['/requisitions', r.id]">{{ r.title }}</a>
                  <span class="sub">{{ r.location }} · {{ employmentTypes[r.employmentType] }} · {{ r.headcount }}
                    {{ r.headcount === 1 ? 'opening' : 'openings' }}</span>
                </td>
                <td>{{ r.department }}</td>
                <td>{{ r.hiringManager.displayName }}</td>
                <td><span class="status" [attr.data-status]="r.status">{{ statusLabels[r.status] }}</span></td>
                <td>{{ r.updatedAt | date: 'mediumDate' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    } @else {
      <p class="muted">Loading requisitions…</p>
    }
  `,
  styles: `
    .filter {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 1rem;
    }
    .filter select {
      width: auto;
    }
    .sub {
      display: block;
      font-size: 0.875rem;
      color: var(--tf-muted);
    }
  `,
})
export class RequisitionListComponent {
  private readonly api = inject(RequisitionsApi);
  private readonly router = inject(Router);
  readonly organizations = inject(OrganizationContext);

  /** `?status=`, bound by the router. */
  readonly status = input<string | undefined>();
  readonly filter = computed(() => parseStatusFilter(this.status()));

  readonly statuses = STATUSES;
  readonly statusLabels = STATUS_LABELS;
  readonly employmentTypes = EMPLOYMENT_TYPE_LABELS;
  readonly requisitions = signal<Requisition[] | null>(null);
  readonly error = signal<string | null>(null);
  readonly emptyMessage = computed(() => {
    const filter = this.filter();
    return filter
      ? `No requisitions are ${STATUS_LABELS[filter].toLowerCase()}.`
      : 'No requisitions yet.';
  });

  constructor() {
    effect((onCleanup) => {
      const organizationId = this.organizations.activeId();
      const filter = this.filter();
      if (!organizationId) {
        return;
      }
      this.requisitions.set(null);
      this.error.set(null);
      const subscription = this.api.list(organizationId, filter).subscribe({
        next: (list) => this.requisitions.set(list),
        error: (error: HttpErrorResponse) => this.error.set(toApiFailure(error).message),
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  changeFilter(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    void this.router.navigate([], { queryParams: { status: value || null }, queryParamsHandling: 'merge' });
  }
}
