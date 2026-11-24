import { Component, computed, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { safeReturnUrl } from '../../core/auth/return-url';
import { toApiFailure } from '../../core/http/api-error';

@Component({
  selector: 'tf-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** Query parameters, bound by the router. */
  readonly returnUrl = input<string | undefined>();
  readonly expired = input<string | undefined>();

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly notice = computed(() =>
    this.expired() && !this.error() ? 'Your session ended. Sign in again to continue.' : null,
  );

  readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email, Validators.maxLength(320)]],
    password: ['', [Validators.required, Validators.maxLength(200)]],
  });

  async submit(): Promise<void> {
    if (this.submitting()) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    try {
      await this.auth.login(email.trim(), password);
      await this.router.navigateByUrl(safeReturnUrl(this.returnUrl()));
    } catch (error: unknown) {
      this.form.controls.password.reset();
      this.error.set(
        error instanceof HttpErrorResponse ? toApiFailure(error).message : 'Something went wrong. Try again.',
      );
    } finally {
      this.submitting.set(false);
    }
  }
}
