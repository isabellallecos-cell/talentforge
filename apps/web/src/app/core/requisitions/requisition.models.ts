/** Shapes returned by `/v1/organizations/{id}/requisitions` (see docs/requisitions.md). */

export type RequisitionStatus = 'draft' | 'pending_approval' | 'approved' | 'rejected';
export type EmploymentType = 'full_time' | 'part_time' | 'contract' | 'internship';
/** `update` edits a draft; the others are `POST …/{id}/{action}`. */
export type RequisitionAction = 'update' | 'submit' | 'withdraw' | 'approve' | 'reject' | 'reopen';
export type StatusAction = Exclude<RequisitionAction, 'update'>;

export interface PersonRef {
  id: string;
  displayName: string;
}

export interface Requisition {
  id: string;
  organizationId: string;
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  headcount: number;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  description: string | null;
  hiringManager: PersonRef;
  status: RequisitionStatus;
  createdBy: PersonRef;
  submittedBy: PersonRef | null;
  submittedAt: string | null;
  decidedBy: PersonRef | null;
  decidedAt: string | null;
  decisionNote: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
  allowedActions: string[];
}

export interface RequisitionEvent {
  id: string;
  action: string;
  fromStatus: RequisitionStatus | null;
  toStatus: RequisitionStatus;
  actor: PersonRef;
  note: string | null;
  at: string;
}

/** Body of `POST …/requisitions` and `PUT …/requisitions/{id}`. */
export interface RequisitionRequest {
  title: string;
  department: string;
  location: string;
  employmentType: EmploymentType;
  headcount: number;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string | null;
  description: string | null;
  hiringManagerId: string;
  version?: number;
}

export const STATUS_LABELS: Record<RequisitionStatus, string> = {
  draft: 'Draft',
  pending_approval: 'Pending approval',
  approved: 'Approved',
  rejected: 'Rejected',
};

export const STATUSES = Object.keys(STATUS_LABELS) as RequisitionStatus[];

export const EMPLOYMENT_TYPE_LABELS: Record<EmploymentType, string> = {
  full_time: 'Full time',
  part_time: 'Part time',
  contract: 'Contract',
  internship: 'Internship',
};

export const EMPLOYMENT_TYPES = Object.keys(EMPLOYMENT_TYPE_LABELS) as EmploymentType[];

export interface ActionInfo {
  action: StatusAction;
  label: string;
  /** Past tense, for the history list. */
  done: string;
  /** Asks for a note before sending; `noteRequired` means the API rejects it without one. */
  asksForNote: boolean;
  noteRequired: boolean;
  /** Shown as a secondary button. */
  secondary: boolean;
}

/** Status actions in the order their buttons appear. */
export const STATUS_ACTIONS: ActionInfo[] = [
  { action: 'submit', label: 'Submit for approval', done: 'Submitted', asksForNote: false, noteRequired: false, secondary: false },
  { action: 'approve', label: 'Approve', done: 'Approved', asksForNote: true, noteRequired: false, secondary: false },
  { action: 'reject', label: 'Reject', done: 'Rejected', asksForNote: true, noteRequired: true, secondary: true },
  { action: 'withdraw', label: 'Withdraw', done: 'Withdrawn', asksForNote: false, noteRequired: false, secondary: true },
  { action: 'reopen', label: 'Reopen as draft', done: 'Reopened', asksForNote: false, noteRequired: false, secondary: true },
];

const HISTORY_LABELS: Record<string, string> = {
  create: 'Created',
  update: 'Edited',
  ...Object.fromEntries(STATUS_ACTIONS.map((info) => [info.action, info.done])),
};

/** Buttons for the status actions the API says this caller can take now. */
export function availableActions(allowed: readonly string[]): ActionInfo[] {
  return STATUS_ACTIONS.filter((info) => allowed.includes(info.action));
}

export function historyLabel(action: string): string {
  return HISTORY_LABELS[action] ?? action;
}

/** "EUR 50,000–70,000 / year", "from EUR 50,000 / year", or null when no salary is set. */
export function formatSalary(
  r: Pick<Requisition, 'salaryMin' | 'salaryMax' | 'salaryCurrency'>,
  locale = 'en-US',
): string | null {
  if (!r.salaryCurrency || (r.salaryMin == null && r.salaryMax == null)) {
    return null;
  }
  const n = (value: number) => value.toLocaleString(locale);
  if (r.salaryMin != null && r.salaryMax != null) {
    return r.salaryMin === r.salaryMax
      ? `${r.salaryCurrency} ${n(r.salaryMin)} / year`
      : `${r.salaryCurrency} ${n(r.salaryMin)}–${n(r.salaryMax)} / year`;
  }
  return r.salaryMin != null
    ? `from ${r.salaryCurrency} ${n(r.salaryMin)} / year`
    : `up to ${r.salaryCurrency} ${n(r.salaryMax!)} / year`;
}

/** Reads `?status=`; anything unknown means "all". */
export function parseStatusFilter(value: string | null | undefined): RequisitionStatus | null {
  return value && (STATUSES as string[]).includes(value) ? (value as RequisitionStatus) : null;
}
