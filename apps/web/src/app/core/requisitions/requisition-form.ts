import { EMPLOYMENT_TYPES, EmploymentType, Requisition, RequisitionRequest } from './requisition.models';

/** Raw values of the requisition form (inputs give strings; numbers may be blank). */
export interface RequisitionFormValue {
  title: string;
  department: string;
  location: string;
  employmentType: string;
  headcount: string | number | null;
  salaryMin: string | number | null;
  salaryMax: string | number | null;
  salaryCurrency: string;
  description: string;
  hiringManagerId: string;
}

export type FieldErrors = Partial<Record<keyof RequisitionFormValue, string>>;

export const EMPTY_FORM: RequisitionFormValue = {
  title: '',
  department: '',
  location: '',
  employmentType: 'full_time',
  headcount: 1,
  salaryMin: null,
  salaryMax: null,
  salaryCurrency: '',
  description: '',
  hiringManagerId: '',
};

export function formValueFrom(r: Requisition): RequisitionFormValue {
  return {
    title: r.title,
    department: r.department,
    location: r.location,
    employmentType: r.employmentType,
    headcount: r.headcount,
    salaryMin: r.salaryMin,
    salaryMax: r.salaryMax,
    salaryCurrency: r.salaryCurrency ?? '',
    description: r.description ?? '',
    hiringManagerId: r.hiringManager.id,
  };
}

function toNumber(value: string | number | null): number | null | 'invalid' {
  if (value === null || (typeof value === 'string' && value.trim() === '')) {
    return null;
  }
  const n = typeof value === 'number' ? value : Number(value.trim());
  return Number.isInteger(n) ? n : 'invalid';
}

function required(value: string, max: number, label: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return `Enter the ${label}.`;
  }
  return trimmed.length > max ? `Use at most ${max} characters.` : undefined;
}

/**
 * Checks the form the same way the API does, so most mistakes are caught
 * before a request. The API stays the authority: its 422 field list is mapped
 * back with {@link fieldErrorsFromApi}.
 */
export function validateRequisitionForm(value: RequisitionFormValue): FieldErrors {
  const errors: FieldErrors = {};
  const set = (field: keyof RequisitionFormValue, message: string | undefined) => {
    if (message) {
      errors[field] = message;
    }
  };
  set('title', required(value.title, 200, 'job title'));
  set('department', required(value.department, 120, 'department'));
  set('location', required(value.location, 120, 'location'));
  if (!(EMPLOYMENT_TYPES as string[]).includes(value.employmentType)) {
    errors.employmentType = 'Choose an employment type.';
  }
  const headcount = toNumber(value.headcount);
  if (headcount === null || headcount === 'invalid' || headcount < 1 || headcount > 50) {
    errors.headcount = 'Enter a whole number from 1 to 50.';
  }
  const min = toNumber(value.salaryMin);
  const max = toNumber(value.salaryMax);
  if (min === 'invalid' || (min !== null && min < 0)) {
    errors.salaryMin = 'Enter a whole amount of 0 or more.';
  }
  if (max === 'invalid' || (max !== null && max < 0)) {
    errors.salaryMax = 'Enter a whole amount of 0 or more.';
  }
  if (!errors.salaryMin && !errors.salaryMax && typeof min === 'number' && typeof max === 'number' && max < min) {
    errors.salaryMax = 'The maximum must be at least the minimum.';
  }
  const currency = value.salaryCurrency.trim().toUpperCase();
  const hasSalary = min !== null || max !== null;
  if (currency && !/^[A-Z]{3}$/.test(currency)) {
    errors.salaryCurrency = 'Use a three-letter code such as EUR.';
  } else if (hasSalary && !currency) {
    errors.salaryCurrency = 'Choose the currency of the salary.';
  } else if (!hasSalary && currency) {
    errors.salaryCurrency = 'Add a minimum or maximum salary, or clear the currency.';
  }
  if (value.description.length > 4000) {
    errors.description = 'Use at most 4000 characters.';
  }
  if (!value.hiringManagerId) {
    errors.hiringManagerId = 'Choose the hiring manager who will approve this requisition.';
  }
  return errors;
}

/** The request body for a valid form; call {@link validateRequisitionForm} first. */
export function toRequisitionRequest(value: RequisitionFormValue, version?: number): RequisitionRequest {
  const num = (v: string | number | null) => {
    const n = toNumber(v);
    return typeof n === 'number' ? n : null;
  };
  const description = value.description.trim();
  const salaryMin = num(value.salaryMin);
  const salaryMax = num(value.salaryMax);
  const request: RequisitionRequest = {
    title: value.title.trim(),
    department: value.department.trim(),
    location: value.location.trim(),
    employmentType: value.employmentType as EmploymentType,
    headcount: num(value.headcount) ?? 0,
    salaryMin,
    salaryMax,
    salaryCurrency: salaryMin !== null || salaryMax !== null ? value.salaryCurrency.trim().toUpperCase() : null,
    description: description || null,
    hiringManagerId: value.hiringManagerId,
  };
  if (version !== undefined) {
    request.version = version;
  }
  return request;
}

const FORM_FIELDS = new Set<string>(Object.keys(EMPTY_FORM));

/**
 * Maps the API's 422 `fields` list onto form fields. Messages for fields the
 * form does not show (e.g. `version`) are returned separately.
 */
export function fieldErrorsFromApi(fields: { field: string; message: string }[]): {
  errors: FieldErrors;
  other: string[];
} {
  const errors: FieldErrors = {};
  const other: string[] = [];
  for (const { field, message } of fields) {
    if (FORM_FIELDS.has(field)) {
      const key = field as keyof RequisitionFormValue;
      errors[key] ??= capitalize(message);
    } else {
      other.push(`${field} ${message}`);
    }
  }
  return { errors, other };
}

function capitalize(message: string): string {
  const text = message.charAt(0).toUpperCase() + message.slice(1);
  return text.endsWith('.') ? text : `${text}.`;
}
