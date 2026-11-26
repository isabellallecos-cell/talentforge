import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { JobPosting } from '../../core/careers/careers.models';
import { JobPostingsApi } from '../../core/careers/careers.service';
import { toApiFailure } from '../../core/http/api-error';
import { OrganizationContext } from '../../core/organization/organization-context.service';

/**
 * Careers-site status of an approved requisition: publish it, see the public
 * page and application count, or close the posting (`jobs:publish`).
 */
@Component({
  selector: 'tf-requisition-posting',
  imports: [DatePipe, RouterLink],
  template: `
    <section class="posting" aria-labelledby="posting-heading">
      <h2 id="posting-heading">Careers site</h2>
      @if (error(); as e) {
        <p class="error" role="alert">{{ e }}</p>
      }
      @if (!loaded()) {
        <p class="muted">Loading…</p>
      } @else if (posting(); as p) {
        @if (p.status === 'open') {
          <p>
            Open since {{ p.publishedAt | date: 'medium' }} (published by {{ p.publishedBy.displayName }}) ·
            {{ p.applicationCount }} {{ p.applicationCount === 1 ? 'application' : 'applications' }}
          </p>
          <div class="actions">
            <a class="button secondary" [routerLink]="p.publicPath">View public page</a>
            @if (canPublish()) {
              <button type="button" class="secondary" [disabled]="busy()" (click)="close(p)">Close posting</button>
            }
          </div>
        } @else {
          <p>
            Closed {{ p.closedAt | date: 'medium' }} by {{ p.closedBy?.displayName }} ·
            {{ p.applicationCount }} {{ p.applicationCount === 1 ? 'application' : 'applications' }}
          </p>
        }
      } @else if (canPublish()) {
        <p class="muted">Not published yet. Publishing lists this job publicly so anyone can apply.</p>
        <button type="button" [disabled]="busy()" (click)="publish()">Publish to careers site</button>
      } @else {
        <p class="muted">Not published yet. A recruiter or admin can publish it.</p>
      }
    </section>
  `,
  styles: `
    .posting {
      margin: 1.5rem 0;
    }
  `,
})
export class RequisitionPostingComponent {
  private readonly api = inject(JobPostingsApi);
  private readonly organizations = inject(OrganizationContext);

  readonly requisitionId = input.required<string>();

  readonly posting = signal<JobPosting | null>(null);
  readonly loaded = signal(false);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);
  readonly canPublish = computed(() => this.organizations.has('jobs:publish'));
  private readonly reloads = signal(0);

  constructor() {
    effect((onCleanup) => {
      const organizationId = this.organizations.activeId();
      const requisitionId = this.requisitionId();
      this.reloads();
      if (!organizationId) {
        return;
      }
      const subscription = this.api.forRequisition(organizationId, requisitionId).subscribe({
        next: (postings) => {
          this.posting.set(postings[0] ?? null);
          this.loaded.set(true);
        },
        error: (error: HttpErrorResponse) => {
          this.loaded.set(true);
          this.error.set(toApiFailure(error).message);
        },
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  publish(): void {
    const organizationId = this.organizations.activeId();
    if (!organizationId || this.busy()) {
      return;
    }
    this.run(this.api.publish(organizationId, this.requisitionId()));
  }

  close(posting: JobPosting): void {
    const organizationId = this.organizations.activeId();
    if (!organizationId || this.busy()) {
      return;
    }
    this.run(this.api.close(organizationId, posting.id, posting.version));
  }

  private run(request: ReturnType<JobPostingsApi['publish']>): void {
    this.busy.set(true);
    this.error.set(null);
    request.subscribe({
      next: (posting) => {
        this.busy.set(false);
        this.posting.set(posting);
      },
      error: (error: HttpErrorResponse) => {
        this.busy.set(false);
        const failure = toApiFailure(error);
        this.error.set(failure.status === 409 ? `${failure.message} The status below has been refreshed.` : failure.message);
        if (failure.status === 409) {
          this.reloads.update((n) => n + 1);
        }
      },
    });
  }
}
