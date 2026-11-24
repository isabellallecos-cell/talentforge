/** Shapes returned by `/v1/auth/*` (see services/api `auth` package). */

export type Role = 'admin' | 'recruiter' | 'hiring_manager' | 'interviewer';

export interface MembershipSummary {
  organizationId: string;
  organizationName: string;
  role: Role;
}

export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
  memberships: MembershipSummary[];
}

export interface CsrfResponse {
  headerName: string;
  token: string;
}

export const ROLE_LABELS: Record<Role, string> = {
  admin: 'Admin',
  recruiter: 'Recruiter',
  hiring_manager: 'Hiring manager',
  interviewer: 'Interviewer',
};
