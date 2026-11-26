import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import {
  ApplicationFieldErrors,
  ApplicationFormValue,
  EMPTY_APPLICATION,
  applicationErrorsFromApi,
  toApplicationRequest,
  validateApplicationForm,
} from '../../core/careers/application-form';
import { ApplicationReceipt, PublicJob } from '../../core/careers/careers.models';
import { CareersApi } from '../../core/careers/careers.service';
import { toApiFailure } from '../../core/http/api-error';
import { EMPLOYMENT_TYPE_LABELS, formatSalary } from '../../core/requisitions/requisition.models';

const CLOSED_MESSAGE = 'This job is no longer open for applications.';

/** One open job and its application form (`/careers/:slug/jobs/:jobId`). No sign-in. */
@Component({
  selector: 'tf-job-posting',
  imports: [DatePipe, FormsModule, RouterLink],
  template: `
    <main id="main" class="careers">
      <p class="demo-banner" role="note">Demo careers site with synthetic jobs. Use fictional details only.</p>
      <p><a [routerLink]="['/careers', slug()]">← All open positions</a></p>

      @if (loadError(); as message) {
        <p class="error" role="alert">{{ message }}</p>
      } @else if (job(); as j) {
        <h1>{{ j.title }}</h1>
        <p class="muted">{{ j.organization.name }} · Posted {{ j.publishedAt | date: 'mediumDate' }}</p>
        <dl class="facts">
          <dt>Department</dt><dd>{{ j.department }}</dd>
          <dt>Location</dt><dd>{{ j.location }}</dd>
          <dt>Employment type</dt><dd>{{ employmentTypes[j.employmentType] }}</dd>
          @if (salary(j); as s) { <dt>Salary</dt><dd>{{ s }}</dd> }
        </dl>
        @if (j.description) {
          <p class="pre">{{ j.description }}</p>
        }

        @if (receipt(); as r) {
          <section class="confirmation" role="status" aria-labelledby="confirmation-heading" tabindex="-1" id="confirmation">
            <h2 id="confirmation-heading">Application received</h2>
            <p>Thank you. {{ r.organizationName }} has your application for {{ r.jobTitle }}.</p>
          </section>
        } @else {
          <h2 id="apply-heading">Apply for this job</h2>
          @if (formError(); as message) {
            <p class="error" role="alert">{{ message }}</p>
          }
          <form class="form-grid" aria-labelledby="apply-heading" (ngSubmit)="submit()" novalidate>
            <div class="field">
              <label for="fullName">Full name</label>
              <input id="fullName" name="fullName" autocomplete="name" maxlength="120" [(ngModel)]="value.fullName"
                [attr.aria-invalid]="!!errors().fullName" aria-describedby="fullName-error" />
              @if (errors().fullName; as e) { <p id="fullName-error" class="field-error">{{ e }}</p> }
            </div>
            <div class="field">
              <label for="email">Email</label>
              <input id="email" name="email" type="email" autocomplete="email" maxlength="254" [(ngModel)]="value.email"
                [attr.aria-invalid]="!!errors().email" aria-describedby="email-error" />
              @if (errors().email; as e) { <p id="email-error" class="field-error">{{ e }}</p> }
            </div>
            <div class="field">
              <label for="phone">Phone (optional)</label>
              <input id="phone" name="phone" type="tel" autocomplete="tel" maxlength="40" [(ngModel)]="value.phone"
                [attr.aria-invalid]="!!errors().phone" aria-describedby="phone-error" />
              @if (errors().phone; as e) { <p id="phone-error" class="field-error">{{ e }}</p> }
            </div>
            <div class="field wide">
              <label for="coverLetter">Why are you interested? (optional)</label>
              <textarea id="coverLetter" name="coverLetter" rows="6" maxlength="4000" [(ngModel)]="value.coverLetter"
                [attr.aria-invalid]="!!errors().coverLetter" aria-describedby="coverLetter-error"></textarea>
              @if (errors().coverLetter; as e) { <p id="coverLetter-error" class="field-error">{{ e }}</p> }
            </div>
            <div class="field wide consent">
              <label>
                <input id="privacyConsent" name="privacyConsent" type="checkbox" [(ngModel)]="value.privacyConsent"
                  [attr.aria-invalid]="!!errors().privacyConsent" aria-describedby="privacyConsent-error" />
                I agree that {{ j.organization.name }} stores these details to consider my application. They are
                visible to its recruiting team only and can be deleted on request.
              </label>
              @if (errors().privacyConsent; as e) { <p id="privacyConsent-error" class="field-error">{{ e }}</p> }
            </div>
            <div class="actions wide">
              <button type="submit" [disabled]="submitting()">{{ submitting() ? 'Sending…' : 'Submit application' }}</button>
            </div>
          </form>
        }
      } @else {
        <p class="muted">Loading job…</p>
      }
    </main>
  `,
  styleUrl: './careers.css',
})
export class JobPostingComponent {
  private readonly api = inject(CareersApi);
  private readonly title = inject(Title);

  /** Route parameters. */
  readonly slug = input.required<string>();
  readonly jobId = input.required<string>();

  readonly employmentTypes = EMPLOYMENT_TYPE_LABELS;
  readonly salary = formatSalary;
  readonly job = signal<PublicJob | null>(null);
  readonly loadError = signal<string | null>(null);
  readonly errors = signal<ApplicationFieldErrors>({});
  readonly formError = signal<string | null>(null);
  readonly submitting = signal(false);
  readonly receipt = signal<ApplicationReceipt | null>(null);
  value: ApplicationFormValue = { ...EMPTY_APPLICATION };

  constructor() {
    effect((onCleanup) => {
      const slug = this.slug();
      const jobId = this.jobId();
      this.job.set(null);
      this.loadError.set(null);
      this.receipt.set(null);
      const subscription = this.api.get(slug, jobId).subscribe({
        next: (job) => {
          this.job.set(job);
          this.title.setTitle(`${job.title} · ${job.organization.name}`);
        },
        error: (error: HttpErrorResponse) =>
          this.loadError.set(
            error.status === 404 || error.status === 400 ? CLOSED_MESSAGE : toApiFailure(error).message,
          ),
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  submit(): void {
    if (this.submitting()) {
      return;
    }
    const errors = validateApplicationForm(this.value);
    this.errors.set(errors);
    this.formError.set(null);
    if (Object.keys(errors).length > 0) {
      this.formError.set('Some fields need attention.');
      focusFirstInvalid();
      return;
    }
    this.submitting.set(true);
    this.api.apply(this.slug(), this.jobId(), toApplicationRequest(this.value)).subscribe({
      next: (receipt) => {
        this.submitting.set(false);
        this.receipt.set(receipt);
        this.value = { ...EMPTY_APPLICATION };
        setTimeout(() => document.getElementById('confirmation')?.focus());
      },
      error: (error: HttpErrorResponse) => {
        this.submitting.set(false);
        const failure = toApiFailure(error);
        if (failure.code === 'validation_failed') {
          const mapped = applicationErrorsFromApi(failure.fields);
          this.errors.set(mapped.errors);
          this.formError.set(['Some fields need attention.', ...mapped.other].join(' '));
          focusFirstInvalid();
        } else if (failure.status === 404) {
          this.formError.set(CLOSED_MESSAGE);
        } else {
          this.formError.set(failure.message);
        }
      },
    });
  }
}

function focusFirstInvalid(): void {
  setTimeout(() => document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());
}
