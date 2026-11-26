import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/auth/auth.guards';
import { CareersListComponent } from './features/careers/careers-list.component';
import { JobPostingComponent } from './features/careers/job-posting.component';
import { HomeComponent } from './features/home/home.component';
import { MembersComponent } from './features/members/members.component';
import { LoginComponent } from './features/login/login.component';
import { NotFoundComponent } from './features/not-found/not-found.component';
import { permissionGuard } from './core/organization/permission.guard';
import { RequisitionDetailComponent } from './features/requisitions/requisition-detail.component';
import { RequisitionFormComponent } from './features/requisitions/requisition-form.component';
import { RequisitionListComponent } from './features/requisitions/requisition-list.component';
import { ShellComponent } from './shell/shell.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard], title: 'Sign in · TalentForge' },
  // Public careers site: no sign-in, outside the app shell.
  { path: 'careers/:slug', component: CareersListComponent, title: 'Careers' },
  { path: 'careers/:slug/jobs/:jobId', component: JobPostingComponent, title: 'Job' },
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
      {
        path: 'requisitions',
        canActivate: [permissionGuard('requisitions:view')],
        children: [
          { path: '', component: RequisitionListComponent, title: 'Requisitions · TalentForge' },
          {
            path: 'new',
            component: RequisitionFormComponent,
            canActivate: [permissionGuard('requisitions:create')],
            title: 'New requisition · TalentForge',
          },
          { path: ':id', component: RequisitionDetailComponent, title: 'Requisition · TalentForge' },
          {
            path: ':id/edit',
            component: RequisitionFormComponent,
            canActivate: [permissionGuard('requisitions:create')],
            title: 'Edit requisition · TalentForge',
          },
        ],
      },
      { path: '**', component: NotFoundComponent, title: 'Not found · TalentForge' },
    ],
  },
];
