import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guards';
import { HomeComponent } from './features/home/home.component';
import { MembersComponent } from './features/members/members.component';
import { LoginComponent } from './features/login/login.component';
import { NotFoundComponent } from './features/not-found/not-found.component';
import { permissionGuard } from './core/organization/permission.guard';
import { ShellComponent } from './shell/shell.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard], title: 'Sign in · TalentForge' },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', component: HomeComponent, title: 'Overview · TalentForge' },
      {
        path: 'members',
        component: MembersComponent,
        canActivate: [permissionGuard('members:view')],
        title: 'Members · TalentForge',
      },
      { path: '**', component: NotFoundComponent, title: 'Not found · TalentForge' },
    ],
  },
];
