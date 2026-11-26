import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { toApiFailure } from '../../core/http/api-error';
import { OrganizationContext } from '../../core/organization/organization-context.service';
import {
  ActionInfo,
  EMPLOYMENT_TYPE_LABELS,
  Requisition,
  RequisitionEvent,
  STATUS_LABELS,
  availableActions,
  formatSalary,
  historyLabel,
} from '../../core/requisitions/requisition.models';
import { RequisitionsApi } from '../../core/requisitions/requisitions.service';
import { RequisitionPostingComponent } from './requisition-posting.component';

/**
 * One requisition, the buttons for the actions the API allows this caller
 * (`allowedActions`), and its history.
 */
@Component({
  selector: 'tf-requisition-detail',
  imports: [DatePipe, FormsModule, RouterLink, RequisitionPostingComponent],
  template: `
    <p><a routerLink="/requisitions">← Requisitions</a></p>

    @if (loadError(); as message) {
      <p class="error" role="alert">{{ message }}</p>
    } @else if (requisition(); as r) {
      <div class="page-header">
        <h1>{{ r.title }}</h1>
        <span class="status" [attr.data-status]="r.status">{{ statusLabels[r.status] }}</span>
      </div>

      @if (message(); as m) {
        <p class="notice" role="status">{{ m }}</p>
      }
      @if (actionError(); as e) {
        <p class="error" role="alert">
          {{ e }}
          @if (stale()) {
            <button type="button" class="secondary" (click)="reload()">Reload</button>
          }
        </p>
      }

      @if (pending(); as info) {
        <form class="decision" (ngSubmit)="confirm()" novalidate>
          <label for="note">
            {{ info.noteRequired ? 'Reason for rejecting (shared with the requester)' : 'Note (optional)' }}
          </label>
          <textarea id="note" name="note" rows="3" maxlength="2000" [(ngModel)]="note"
            [attr.aria-invalid]="!!noteError()" aria-describedby="note-error"></textarea>
          @if (noteError(); as e) { <p id="note-error" class="field-error">{{ e }}</p> }
          <div class="actions">
            <button type="submit" [class.danger]="info.action === 'reject'" [disabled]="busy()">{{ info.label }}</button>
            <button type="button" class="secondary" (click)="cancel()" [disabled]="busy()">Cancel</button>
          </div>
        </form>
      } @else if (actions().length > 0 || canEdit()) {
        <div class="actions" role="group" aria-label="Actions">
          @if (canEdit()) {
            <a class="button secondary" [routerLink]="['/requisitions', r.id, 'edit']">Edit</a>
          }
          @for (info of actions(); track info.action) {
            <button type="button" [class.secondary]="info.secondary" [disabled]="busy()" (click)="start(info)">
              {{ info.label }}
            </button>
          }
        </div>
      } @else if (waitingText(); as text) {
        <p class="muted">{{ text }}</p>
      }

      <dl class="facts">
        <dt>Department</dt><dd>{{ r.department }}</dd>
        <dt>Location</dt><dd>{{ r.location }}</dd>
        <dt>Employment type</dt><dd>{{ employmentTypes[r.employmentType] }}</dd>
        <dt>Openings</dt><dd>{{ r.headcount }}</dd>
        <dt>Salary</dt><dd>{{ salary() ?? 'Not set' }}</dd>
        <dt>Hiring manager</dt><dd>{{ r.hiringManager.displayName }}</dd>
        <dt>Created by</dt><dd>{{ r.createdBy.displayName }} · {{ r.createdAt | date: 'medium' }}</dd>
        @if (r.submittedBy) {
          <dt>Submitted by</dt><dd>{{ r.submittedBy.displayName }} · {{ r.submittedAt | date: 'medium' }}</dd>
        }
        @if (r.decidedBy) {
          <dt>{{ r.status === 'rejected' ? 'Rejected by' : 'Approved by' }}</dt>
          <dd>{{ r.decidedBy.displayName }} · {{ r.decidedAt | date: 'medium' }}</dd>
        }
        @if (r.decisionNote) {
          <dt>{{ r.status === 'rejected' ? 'Reason' : 'Decision note' }}</dt><dd class="pre">{{ r.decisionNote }}</dd>
        }
      </dl>

      @if (r.status === 'approved') {
        <tf-requisition-posting [requisitionId]="r.id" />
      }

      @if (r.description) {
        <h2>Description</h2>
        <p class="pre">{{ r.description }}</p>
      }

      <h2 id="history-heading">History</h2>
      <ol class="history" aria-labelledby="history-heading">
        @for (event of events(); track event.id) {
          <li>
            <strong>{{ historyLabel(event.action) }}</strong> by {{ event.actor.displayName }}
            <time [attr.datetime]="event.at">{{ event.at | date: 'medium' }}</time>
            @if (event.fromStatus && event.fromStatus !== event.toStatus) {
              <span class="muted">({{ statusLabels[event.fromStatus] }} → {{ statusLabels[event.toStatus] }})</span>
            }
            @if (event.note) {
              <p class="pre">{{ event.note }}</p>
            }
          </li>
        }
      </ol>
    } @else {
      <p class="muted">Loading requisition…</p>
    }
  `,
  styles: `
    .facts {
      display: grid;
      grid-template-columns: max-content 1fr;
      gap: 0.375rem 1.5rem;
      margin: 1.5rem 0;
    }
    dt {
      color: var(--tf-muted);
    }
    dd {
      margin: 0;
    }
    .pre {
      white-space: pre-wrap;
    }
    .decision {
      max-width: 36rem;
      padding: 1rem;
      background: var(--tf-surface);
      border: 1px solid var(--tf-border);
      border-radius: 0.375rem;
    }
    .history {
      padding-left: 1.25rem;
    }
    .history li {
      margin-bottom: 0.75rem;
    }
    .history time,
    .history .muted {
      margin-left: 0.25rem;
      color: var(--tf-muted);
    }
    .history p {
      margin: 0.25rem 0 0;
    }
  `,
})
export class RequisitionDetailComponent {
  private readonly api = inject(RequisitionsApi);
  private readonly organizations = inject(OrganizationContext);

  /** Route parameter. */
  readonly id = input.required<string>();

  readonly statusLabels = STATUS_LABELS;
  readonly employmentTypes = EMPLOYMENT_TYPE_LABELS;
  readonly historyLabel = historyLabel;

  readonly requisition = signal<Requisition | null>(null);
  readonly events = signal<RequisitionEvent[]>([]);
  readonly loadError = signal<string | null>(null);
  readonly pending = signal<ActionInfo | null>(null);
  readonly noteError = signal<string | null>(null);
  readonly actionError = signal<string | null>(null);
  readonly stale = signal(false);
  readonly message = signal<string | null>(null);
  readonly busy = signal(false);
  note = '';
  private readonly reloads = signal(0);

  readonly actions = computed(() => availableActions(this.requisition()?.allowedActions ?? []));
  readonly canEdit = computed(() => this.requisition()?.allowedActions.includes('update') ?? false);
  readonly salary = computed(() => {
    const r = this.requisition();
    return r ? formatSalary(r) : null;
  });
  readonly waitingText = computed(() => {
    const r = this.requisition();
    return r?.status === 'pending_approval' ? `Waiting for ${r.hiringManager.displayName} to decide.` : null;
  });

  constructor() {
    effect((onCleanup) => {
      const organizationId = this.organizations.activeId();
      const id = this.id();
      this.reloads();
      if (!organizationId) {
        return;
      }
      untracked(() => {
        this.loadError.set(null);
        this.actionError.set(null);
        this.stale.set(false);
        this.pending.set(null);
      });
      const subscription = forkJoin({
        requisition: this.api.get(organizationId, id),
        events: this.api.history(organizationId, id),
      }).subscribe({
        next: ({ requisition, events }) => {
          this.requisition.set(requisition);
          this.events.set(events);
        },
        error: (error: HttpErrorResponse) => {
          this.requisition.set(null);
          this.loadError.set(toApiFailure(error).message);
        },
      });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  start(info: ActionInfo): void {
    this.actionError.set(null);
    this.message.set(null);
    if (info.asksForNote) {
      this.note = '';
      this.noteError.set(null);
      this.pending.set(info);
      setTimeout(() => document.getElementById('note')?.focus());
      return;
    }
    this.run(info, null);
  }

  confirm(): void {
    const info = this.pending();
    if (!info) {
      return;
    }
    const note = this.note.trim();
    if (info.noteRequired && !note) {
      this.noteError.set('Explain why, so the requester knows what to change.');
      document.getElementById('note')?.focus();
      return;
    }
    this.run(info, note || null);
  }

  cancel(): void {
    this.pending.set(null);
    this.noteError.set(null);
  }

  reload(): void {
    this.message.set(null);
    this.reloads.update((n) => n + 1);
  }

  private run(info: ActionInfo, note: string | null): void {
    const organizationId = this.organizations.activeId();
    const r = this.requisition();
    if (!organizationId || !r || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.api.transition(organizationId, r.id, info.action, r.version, note).subscribe({
      next: (updated) => {
        this.busy.set(false);
        this.pending.set(null);
        this.requisition.set(updated);
        this.message.set(`${info.done}. The requisition is now ${STATUS_LABELS[updated.status].toLowerCase()}.`);
        this.api.history(organizationId, r.id).subscribe({ next: (events) => this.events.set(events) });
      },
      error: (error: HttpErrorResponse) => {
        this.busy.set(false);
        const failure = toApiFailure(error);
        this.stale.set(failure.code === 'stale_version' || failure.code === 'invalid_transition');
        if (failure.code === 'validation_failed' && info.noteRequired) {
          this.noteError.set(failure.message);
          return;
        }
        this.pending.set(null);
        this.actionError.set(
          this.stale()
            ? 'This requisition was changed by someone else. Reload to see its current state.'
            : failure.message,
        );
      },
    });
  }
}
