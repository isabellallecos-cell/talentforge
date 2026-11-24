import { describe, expect, it } from 'vitest';

import {
  EMPTY_FORM,
  RequisitionFormValue,
  fieldErrorsFromApi,
  formValueFrom,
  toRequisitionRequest,
  validateRequisitionForm,
} from './requisition-form';
import { Requisition } from './requisition.models';

const VALID: RequisitionFormValue = {
  ...EMPTY_FORM,
  title: '  Backend Engineer ',
  department: 'Engineering',
  location: 'Remote (EU)',
  headcount: '2',
  hiringManagerId: '7a0e8c1e-0000-4000-8000-000000000001',
};

describe('validateRequisitionForm', () => {
  it('accepts a complete form without salary', () => {
    expect(validateRequisitionForm(VALID)).toEqual({});
  });

  it('requires title, department, location and hiring manager', () => {
    const errors = validateRequisitionForm({ ...EMPTY_FORM, title: '   ' });
    expect(Object.keys(errors).sort()).toEqual(['department', 'hiringManagerId', 'location', 'title']);
  });

  it('enforces the API length limits', () => {
    const errors = validateRequisitionForm({ ...VALID, title: 'x'.repeat(201), description: 'y'.repeat(4001) });
    expect(errors.title).toMatch(/200/);
    expect(errors.description).toMatch(/4000/);
  });

  it.each([['0'], ['51'], ['1.5'], [''], ['abc'], [null]])('rejects headcount %s', (headcount) => {
    expect(validateRequisitionForm({ ...VALID, headcount }).headcount).toBeDefined();
  });

  it('accepts headcount given as a number', () => {
    expect(validateRequisitionForm({ ...VALID, headcount: 50 }).headcount).toBeUndefined();
  });

  it('needs a currency when a salary is given, and a salary when a currency is given', () => {
    expect(validateRequisitionForm({ ...VALID, salaryMin: 50000 }).salaryCurrency).toMatch(/currency/);
    expect(validateRequisitionForm({ ...VALID, salaryCurrency: 'EUR' }).salaryCurrency).toMatch(/minimum or maximum/);
    expect(validateRequisitionForm({ ...VALID, salaryMax: '80000', salaryCurrency: 'eur' })).toEqual({});
  });

  it('checks the salary range and currency format', () => {
    const errors = validateRequisitionForm({ ...VALID, salaryMin: 90000, salaryMax: 80000, salaryCurrency: 'EU' });
    expect(errors.salaryMax).toMatch(/at least the minimum/);
    expect(errors.salaryCurrency).toMatch(/three-letter/);
    expect(validateRequisitionForm({ ...VALID, salaryMin: -1, salaryCurrency: 'EUR' }).salaryMin).toBeDefined();
  });

  it('rejects unknown employment types', () => {
    expect(validateRequisitionForm({ ...VALID, employmentType: 'freelance' }).employmentType).toBeDefined();
  });
});

describe('toRequisitionRequest', () => {
  it('trims text, converts numbers and drops an empty description', () => {
    expect(toRequisitionRequest({ ...VALID, description: '  ' })).toEqual({
      title: 'Backend Engineer',
      department: 'Engineering',
      location: 'Remote (EU)',
      employmentType: 'full_time',
      headcount: 2,
      salaryMin: null,
      salaryMax: null,
      salaryCurrency: null,
      description: null,
      hiringManagerId: VALID.hiringManagerId,
    });
  });

  it('upper-cases the currency and includes the version when editing', () => {
    const request = toRequisitionRequest({ ...VALID, salaryMin: '50000', salaryMax: '', salaryCurrency: ' eur ' }, 3);
    expect(request).toMatchObject({ salaryMin: 50000, salaryMax: null, salaryCurrency: 'EUR', version: 3 });
  });

  it('round-trips a stored requisition', () => {
    const stored = {
      title: 'Data Analyst',
      department: 'Finance',
      location: 'Madrid',
      employmentType: 'contract',
      headcount: 1,
      salaryMin: 40000,
      salaryMax: 52000,
      salaryCurrency: 'EUR',
      description: 'Quarterly reporting.',
      hiringManager: { id: 'm1', displayName: 'Morgan Diaz' },
      version: 4,
    } as Requisition;
    const value = formValueFrom(stored);
    expect(validateRequisitionForm(value)).toEqual({});
    expect(toRequisitionRequest(value, stored.version)).toEqual({
      title: 'Data Analyst',
      department: 'Finance',
      location: 'Madrid',
      employmentType: 'contract',
      headcount: 1,
      salaryMin: 40000,
      salaryMax: 52000,
      salaryCurrency: 'EUR',
      description: 'Quarterly reporting.',
      hiringManagerId: 'm1',
      version: 4,
    });
  });
});

describe('fieldErrorsFromApi', () => {
  it('maps known fields and keeps the first message per field', () => {
    const { errors, other } = fieldErrorsFromApi([
      { field: 'salaryMax', message: 'must be greater than or equal to salaryMin' },
      { field: 'salaryMax', message: 'must be greater than or equal to 0' },
      { field: 'version', message: 'must not be null' },
    ]);
    expect(errors).toEqual({ salaryMax: 'Must be greater than or equal to salaryMin.' });
    expect(other).toEqual(['version must not be null']);
  });
});
