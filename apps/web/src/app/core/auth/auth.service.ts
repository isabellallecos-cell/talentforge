import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { CurrentUser } from './auth.models';

/**
 * Holds the signed-in user. The session itself is an HttpOnly cookie, so the
 * only way to know whether we are signed in is to ask `/v1/auth/me`.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly userState = signal<CurrentUser | null>(null);
  private restore: Promise<CurrentUser | null> | null = null;

  readonly user = this.userState.asReadonly();
  readonly signedIn = computed(() => this.userState() !== null);

  /** Loads the current session once; later calls reuse the answer. */
  restoreSession(): Promise<CurrentUser | null> {
    this.restore ??= firstValueFrom(this.http.get<CurrentUser>('/v1/auth/me')).then(
      (user) => {
        this.userState.set(user);
        return user;
      },
      (error: unknown) => {
        this.restore = null;
        if (error instanceof HttpErrorResponse && error.status === 401) {
          this.userState.set(null);
          return null;
        }
        throw error;
      },
    );
    return this.restore;
  }

  async login(email: string, password: string): Promise<CurrentUser> {
    const user = await firstValueFrom(this.http.post<CurrentUser>('/v1/auth/login', { email, password }));
    this.userState.set(user);
    this.restore = Promise.resolve(user);
    return user;
  }

  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/v1/auth/logout', null));
    } finally {
      this.clear();
    }
  }

  /** Forgets the user locally, e.g. after the server reports an expired session. */
  clear(): void {
    this.userState.set(null);
    this.restore = Promise.resolve(null);
  }
}
