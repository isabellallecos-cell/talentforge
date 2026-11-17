import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'tf-root',
  imports: [RouterOutlet],
  template: `
    <header>
      <h1>TalentForge</h1>
    </header>
    <main>
      <router-outlet />
    </main>
  `,
  styles: `
    header {
      padding: 1rem 1.5rem;
      background: #ffffff;
      border-bottom: 1px solid #d9dee5;
    }
    h1 {
      margin: 0;
      font-size: 1.25rem;
    }
    main {
      padding: 1.5rem;
    }
  `,
})
export class AppComponent {}
