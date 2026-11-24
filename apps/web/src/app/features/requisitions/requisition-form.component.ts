import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';

import { toApiFailure } from '../../core/http/api-error';
import { OrganizationContext } from '../../core/organization/organization-context.service';
import {
  EMPTY_FORM,
  FieldErrors,
  RequisitionFormValue,
  fieldErrorsFromApi,
  formValueFrom,
  toRequisitionRequest,
  validateRequisitionForm,
} from '../../core/requisitions/requisition-form';
import { EMPLOYMENT_TYPES, EMPLOYMENT_TYPE_LABELS, PersonRef, Requisition } from '../../core/requisitions/requisition.models';
import { RequisitionsApi } from '../../core/requisitions/requisitions.service';

/** Create a requisition (`/requisitions/new`) or edit a draft (`/requisitions/:id/edit`). */
@Component({
  selector: 'tf-requisition-form',
  imports: [FormsModule, RouterLink],
  template: `
    <h1>{{ id() ? 'Edit requisition' : 'New requisition' }}</h1>

    @if (loadError(); as message) {
      <p class="error" role="alert">{{ message }}</p>
      <p><a routerLink="/requisitions">Back to requisitions</a></p>
    } @else if (!ready()) {
      <p class="muted">Loading…</p>
    } @else if (existing() && !existing()!.allowedActions.includes('update')) {
      <p class="notice" role="status">Only drafts can be edited, and this requisition is no longer a draft.</p>
      <p><a [routerLink]="['/requisitions', id()]">View the requisition</a></p>
    } @else {
      @if (managers().length === 0) {
        <p class="notice" role="status">
          This organization has no hiring manager or admin to approve requisitions yet.
        </p>
      }
      @if (formError(); as message) {
        <p class="error" role="alert">{{ message }}</p>
      }
      <form class="form-grid" (ngSubmit)="save()" novalidate>
        <div class="field wide">
          <label for="title">Job title</label>
          <input id="title" name="title" [(ngModel)]="value.title" maxlength="200" [attr.aria-invalid]="!!errors().title"
            aria-describedby="title-error" />
          @if (errors().title; as e) { <p id="title-error" class="field-error">{{ e }}</p> }
        </div>

        <div class="field">
          <label for="department">Department</label>
          <input id="department" name="department" [(ngModel)]="value.department" maxlength="120"
            [attr.aria-invalid]="!!errors().department" aria-describedby="department-error" />
          @if (errors().department; as e) { <p id="department-error" class="field-error">{{ e }}</p> }
        </div>

        <div class="field">
          <label for="location">Location</label>
          <input id="location" name="location" [(ngModel)]="value.location" maxlength="120"
            [attr.aria-invalid]="!!errors().location" aria-describedby="location-error" />
          @if (errors().location; as e) { <p id="location-error" class="field-error">{{ e }}</p> }
        </div>

        <div class="field">
          <label for="employmentType">Employment type</label>
          <select id="employmentType" name="employmentType" [(ngModel)]="value.employmentType">
            @for (type of employmentTypes; track type) {
              <option [value]="type">{{ employmentTypeLabels[type] }}</option>
            }
          </select>
        </div>

        <div class="field">
          <label for="headcount">Openings</label>
          <input id="headcount" name="headcount" type="number" min="1" max="50" step="1" [(ngModel)]="value.headcount"
            [attr.aria-invalid]="!!errors().headcount" aria-describedby="headcount-error" />
          @if (errors().headcount; as e) { <p id="headcount-error" class="field-error">{{ e }}</p> }
        </div>

        <fieldset class="wide salary">
          <legend>Yearly salary (optional)</legend>
          <div class="field">
            <label for="salaryMin">Minimum</label>
            <input id="salaryMin" name="salaryMin" type="number" min="0" step="1" [(ngModel)]="value.salaryMin"
              [attr.aria-invalid]="!!errors().salaryMin" aria-describedby="salaryMin-error" />
            @if (errors().salaryMin; as e) { <p id="salaryMin-error" class="field-error">{{ e }}</p> }
          </div>
          <div class="field">
            <label for="salaryMax">Maximum</label>
            <input id="salaryMax" name="salaryMax" type="number" min="0" step="1" [(ngModel)]="value.salaryMax"
              [attr.aria-invalid]="!!errors().salaryMax" aria-describedby="salaryMax-error" />
            @if (errors().salaryMax; as e) { <p id="salaryMax-error" class="field-error">{{ e }}</p> }
          </div>
          <div class="field">
            <label for="salaryCurrency">Currency</label>
            <input id="salaryCurrency" name="salaryCurrency" maxlength="3" placeholder="EUR" autocomplete="off"
              [(ngModel)]="value.salaryCurrency" [attr.aria-invalid]="!!errors().salaryCurrency"
              aria-describedby="salaryCurrency-error" />
            @if (errors().salaryCurrency; as e) { <p id="salaryCurrency-error" class="field-error">{{ e }}</p> }
          </div>
        </fieldset>

        <div class="field wide">
          <label for="hiringManagerId">Hiring manager</label>
          <select id="hiringManagerId" name="hiringManagerId" [(ngModel)]="value.hiringManagerId"
            [attr.aria-invalid]="!!errors().hiringManagerId" aria-describedby="hiringManagerId-hint hiringManagerId-error">
            <option value="">Choose…</option>
            @for (manager of managers(); track manager.id) {
              <option [value]="manager.id">{{ manager.displayName }}</option>
            }
          </select>
          <p id="hiringManagerId-hint" class="hint">They approve or reject the requisition once it is submitted.</p>
          @if (errors().hiringManagerId; as e) { <p id="hiringManagerId-error" class="field-error">{{ e }}</p> }
        </div>

        <div class="field wide">
          <label for="description">Description (optional)</label>
          <textarea id="description" name="description" rows="6" maxlength="4000" [(ngModel)]="value.description"
            [attr.aria-invalid]="!!errors().description" aria-describedby="description-error"></textarea>
          @if (errors().description; as e) { <p id="description-error" class="field-error">{{ e }}</p> }
        </div>

        <div class="actions wide">
          <button type="submit" [disabled]="saving()">{{ saving() ? 'Saving…' : id() ? 'Save draft' : 'Create draft' }}</button>
          <a class="button secondary" [routerLink]="id() ? ['/requisitions', id()] : ['/requisitions']">Cancel</a>
        </div>
      </form>
    }
  `,
  styles: `
    .salary {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 0 1rem;
      margin: 0;
      padding: 0.75rem 1rem;
      border: 1px solid var(--tf-border);
      border-radius: 0.375rem;
    }
  `,
})
export class RequisitionFormComponent {
  private readonly api = inject(RequisitionsApi);
  private readonly router = inject(Router);
  private readonly organizations = inject(OrganizationContext);

  /** Route parameter; absent when creating. */
  readonly id = input<string | undefined>();

  readonly employmentTypes = EMPLOYMENT_TYPES;
  readonly employmentTypeLabels = EMPLOYMENT_TYPE_LABELS;

  value: RequisitionFormValue = { ...EMPTY_FORM };
  readonly existing = signal<Requisition | null>(null);
  readonly managers = signal<PersonRef[]>([]);
  readonly loaded = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly ready = computed(() => this.loaded() && !this.loadError());
  readonly errors = signal<FieldErrors>({});
  readonly formError = signal<string | null>(null);
  readonly saving = signal(false);

  constructor() {
    effect((onCleanup) => {
      const organizationId = this.organizations.activeId();
      const id = this.id();
      if (!organizationId) {
        return;
      }
      untracked(() => {
        this.loaded.set(false);
        this.loadError.set(null);
        this.errors.set({});
        this.formError.set(null);
      });
      const subscription = forkJoin({
        managers: this.api.hiringManagers(organizationId),
        requisition: id ? this.api.get(organizationId, id) : of(null),
      }).subscribe({
        next: ({ managers, requisition }) => {
          this.managers.set(managers);
          this.existing.set(requisition);
          this.value = requisition ? formValueFrom(requisition) : { ...EMPTY_FORM };
          if (!requisition && managers.length === 1) {
            this.value.hiringManagerId = managers[0].id;
          }
          this.loaded.set(true);
        },
        error: (error: HttpErrorResponse) => {
          this.loadError.set(toApiFailure(error).message);
          this.loaded.set(true);
        },
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  save(): void {
    const organizationId = this.organizations.activeId();
    if (this.saving() || !organizationId) {
      return;
    }
    const errors = validateRequisitionForm(this.value);
    this.errors.set(errors);
    this.formError.set(null);
    if (Object.keys(errors).length > 0) {
      this.formError.set('Some fields need attention.');
      focusFirstInvalid();
      return;
    }
    const existing = this.existing();
    const request = toRequisitionRequest(this.value, existing?.version);
    this.saving.set(true);
    const call = existing
      ? this.api.update(organizationId, existing.id, request)
      : this.api.create(organizationId, request);
    call.subscribe({
      next: (saved) => {
        this.saving.set(false);
        void this.router.navigate(['/requisitions', saved.id]);
      },
      error: (error: HttpErrorResponse) => {
        this.saving.set(false);
        const failure = toApiFailure(error);
        if (failure.code === 'validation_failed' && failure.fields.length > 0) {
          const mapped = fieldErrorsFromApi(failure.fields);
          this.errors.set(mapped.errors);
          this.formError.set(['Some fields need attention.', ...mapped.other].join(' '));
          focusFirstInvalid();
        } else if (failure.code === 'stale_version') {
          this.formError.set(
            'Someone else changed this requisition while you were editing. Reload the page to see their changes; your edits were not saved.',
          );
        } else {
          this.formError.set(failure.message);
        }
      },
    });
  }
}

function focusFirstInvalid(): void {
  // After the errors render.
  setTimeout(() => document.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus());
}
