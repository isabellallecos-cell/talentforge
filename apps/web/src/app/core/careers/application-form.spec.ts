import { describe, expect, it } from 'vitest';

import {
  ApplicationFormValue,
  EMPTY_APPLICATION,
  applicationErrorsFromApi,
  toApplicationRequest,
  validateApplicationForm,
} from './application-form';

const VALID: ApplicationFormValue = {
  fullName: '  Ada Example ',
  email: ' ada@example.org ',
  phone: '',
  coverLetter: '',
  privacyConsent: true,
};

describe('validateApplicationForm', () => {
  it('accepts name, email and consent alone', () => {
    expect(validateApplicationForm(VALID)).toEqual({});
  });

  it('requires name, email and consent', () => {
    expect(Object.keys(validateApplicationForm(EMPTY_APPLICATION)).sort()).toEqual([
      'email',
      'fullName',
      'privacyConsent',
    ]);
  });

  it.each([['ada'], ['ada@example'], ['a da@example.org'], ['ada@@example.org'], [`${'a'.repeat(250)}@x.org`]])(
    'rejects email %s',
    (email) => {
      expect(validateApplicationForm({ ...VALID, email }).email).toBeDefined();
    },
  );

  it('rejects control characters in the name', () => {
    expect(validateApplicationForm({ ...VALID, fullName: 'Ada\nExample' }).fullName).toMatch(/line breaks/);
  });

  it('checks phone characters and length', () => {
    expect(validateApplicationForm({ ...VALID, phone: '+34 (600) 000-000' })).toEqual({});
    expect(validateApplicationForm({ ...VALID, phone: 'call me' }).phone).toBeDefined();
    expect(validateApplicationForm({ ...VALID, phone: '1'.repeat(41) }).phone).toMatch(/40/);
  });

  it('enforces the API length limits', () => {
    const errors = validateApplicationForm({ ...VALID, fullName: 'x'.repeat(121), coverLetter: 'y'.repeat(4001) });
    expect(errors.fullName).toMatch(/120/);
    expect(errors.coverLetter).toMatch(/4000/);
  });
});

describe('toApplicationRequest', () => {
  it('trims values and sends blanks as null', () => {
    expect(toApplicationRequest({ ...VALID, coverLetter: '  \n ' })).toEqual({
      fullName: 'Ada Example',
      email: 'ada@example.org',
      phone: null,
      coverLetter: null,
      privacyConsent: true,
    });
  });
});

describe('applicationErrorsFromApi', () => {
  it('maps known fields and keeps the first message per field', () => {
    const { errors, other } = applicationErrorsFromApi([
      { field: 'email', message: 'must be a valid email address' },
      { field: 'email', message: 'size must be between 0 and 254' },
      { field: 'privacyConsent', message: 'must be accepted to apply' },
      { field: 'resume', message: 'is not supported' },
    ]);
    expect(errors).toEqual({ email: 'Must be a valid email address.', privacyConsent: 'Must be accepted to apply.' });
    expect(other).toEqual(['resume is not supported']);
  });
});
