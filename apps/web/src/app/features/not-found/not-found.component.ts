import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'tf-not-found',
  imports: [RouterLink],
  template: `
    <h1>Page not found</h1>
    <p>This page does not exist or you do not have access to it.</p>
    <p><a routerLink="/">Back to the overview</a></p>
  `,
})
export class NotFoundComponent {}
