import { DatePipe } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';

import { ROLE_LABELS, Role } from '../../core/auth/auth.models';
import { toApiFailure } from '../../core/http/api-error';
import { OrganizationContext } from '../../core/organization/organization-context.service';

interface Member {
  membershipId: string;
  userId: string;
  displayName: string;
  email: string;
  role: Role;
  joinedAt: string;
}

@Component({
  selector: 'tf-members',
  imports: [DatePipe],
  template: `
    <h1>Members</h1>
    @if (error(); as message) {
      <p class="error" role="alert">{{ message }}</p>
    } @else if (members(); as members) {
      <table>
        <caption class="visually-hidden">Members of {{ organizations.active()?.organizationName }}</caption>
        <thead>
          <tr>
            <th scope="col">Name</th>
            <th scope="col">Email</th>
            <th scope="col">Role</th>
            <th scope="col">Joined</th>
          </tr>
        </thead>
        <tbody>
          @for (member of members; track member.membershipId) {
            <tr>
              <td>{{ member.displayName }}</td>
              <td>{{ member.email }}</td>
              <td>{{ roles[member.role] }}</td>
              <td>{{ member.joinedAt | date: 'mediumDate' }}</td>
            </tr>
          }
        </tbody>
      </table>
    } @else {
      <p class="muted">Loading members…</p>
    }
  `,
  styles: `
    table {
      width: 100%;
      border-collapse: collapse;
      background: var(--tf-surface);
    }
    th,
    td {
      padding: 0.5rem 0.75rem;
      text-align: left;
      border-bottom: 1px solid var(--tf-border);
    }
    .muted {
      color: var(--tf-muted);
    }
    .error {
      color: var(--tf-danger);
    }
  `,
})
export class MembersComponent {
  private readonly http = inject(HttpClient);
  readonly organizations = inject(OrganizationContext);
  readonly roles = ROLE_LABELS;
  readonly members = signal<Member[] | null>(null);
  readonly error = signal<string | null>(null);

  constructor() {
    effect((onCleanup) => {
      const id = this.organizations.activeId();
      if (!id) {
        return;
      }
      this.members.set(null);
      this.error.set(null);
      const subscription = this.http
        .get<Member[]>(`/v1/organizations/${encodeURIComponent(id)}/members`)
        .subscribe({
          next: (members) => this.members.set(members),
          error: (error: HttpErrorResponse) => this.error.set(toApiFailure(error).message),
        });
      onCleanup(() => subscription.unsubscribe());
    });
  }
}
