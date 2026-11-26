import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { PublicJobList } from '../../core/careers/careers.models';
import { CareersApi } from '../../core/careers/careers.service';
import { toApiFailure } from '../../core/http/api-error';
import { EMPLOYMENT_TYPE_LABELS, formatSalary } from '../../core/requisitions/requisition.models';

/** Public list of an organization's open jobs (`/careers/:slug`). No sign-in. */
@Component({
  selector: 'tf-careers-list',
  imports: [DatePipe, RouterLink],
  template: `
    <main id="main" class="careers">
      <p class="demo-banner" role="note">Demo careers site with synthetic jobs. Use fictional details only.</p>
      @if (error(); as message) {
        <h1>Careers</h1>
        <p class="error" role="alert">{{ message }}</p>
      } @else if (list(); as l) {
        <h1>Careers at {{ l.organization.name }}</h1>
        @if (l.jobs.length === 0) {
          <p class="muted">There are no open positions right now. Check back later.</p>
        } @else {
          <p class="muted">{{ l.jobs.length }} open {{ l.jobs.length === 1 ? 'position' : 'positions' }}</p>
          <ul class="jobs">
            @for (job of l.jobs; track job.id) {
              <li>
                <h2><a [routerLink]="['/careers', l.organization.slug, 'jobs', job.id]">{{ job.title }}</a></h2>
                <p>
                  {{ job.department }} · {{ job.location }} · {{ employmentTypes[job.employmentType] }}
                  @if (salary(job); as s) { · {{ s }} }
                </p>
                <p class="muted">Posted {{ job.publishedAt | date: 'mediumDate' }}</p>
              </li>
            }
          </ul>
        }
      } @else {
        <p class="muted">Loading open positions…</p>
      }
    </main>
  `,
  styleUrl: './careers.css',
})
export class CareersListComponent {
  private readonly api = inject(CareersApi);
  private readonly title = inject(Title);

  /** Route parameter. */
  readonly slug = input.required<string>();

  readonly employmentTypes = EMPLOYMENT_TYPE_LABELS;
  readonly salary = formatSalary;
  readonly list = signal<PublicJobList | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const slug = this.slug();
      this.list.set(null);
      this.error.set(null);
      const subscription = this.api.list(slug).subscribe({
        next: (list) => {
          this.list.set(list);
          this.title.setTitle(`Careers at ${list.organization.name}`);
        },
        error: (error: HttpErrorResponse) =>
          this.error.set(error.status === 404 ? 'This careers page does not exist.' : toApiFailure(error).message),
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }
}
