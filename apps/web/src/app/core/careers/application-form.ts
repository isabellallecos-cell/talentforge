import { ApplicationRequest } from './careers.models';

/** Raw values of the public application form. */
export interface ApplicationFormValue {
  fullName: string;
  email: string;
  phone: string;
  coverLetter: string;
  privacyConsent: boolean;
}

export type ApplicationFieldErrors = Partial<Record<keyof ApplicationFormValue, string>>;

export const EMPTY_APPLICATION: ApplicationFormValue = {
  fullName: '',
  email: '',
  phone: '',
  coverLetter: '',
  privacyConsent: false,
};

// Same rules as ApplicationRequest on the server.
const EMAIL = /^[^@\s]+@[^@\s]+\.[^@\s]+$/;
const PHONE = /^[0-9+()\-. ]*$/;
const CONTROL = /[\u0000-\u001f\u007f]/;

/**
 * Checks the form the same way the API does. The API stays the authority:
 * its 422 field list is mapped back with {@link applicationErrorsFromApi}.
 */
export function validateApplicationForm(value: ApplicationFormValue): ApplicationFieldErrors {
  const errors: ApplicationFieldErrors = {};
  const name = value.fullName.trim();
  if (!name) {
    errors.fullName = 'Enter your full name.';
  } else if (name.length > 120) {
    errors.fullName = 'Use at most 120 characters.';
  } else if (CONTROL.test(name)) {
    errors.fullName = 'Remove line breaks and special characters.';
  }
  const email = value.email.trim();
  if (!email) {
    errors.email = 'Enter your email address.';
  } else if (email.length > 254 || !EMAIL.test(email)) {
    errors.email = 'Enter an email address like name@example.org.';
  }
  const phone = value.phone.trim();
  if (phone.length > 40) {
    errors.phone = 'Use at most 40 characters.';
  } else if (!PHONE.test(phone)) {
    errors.phone = 'Use digits, spaces and + ( ) - . only.';
  }
  if (value.coverLetter.length > 4000) {
    errors.coverLetter = 'Use at most 4000 characters.';
  }
  if (!value.privacyConsent) {
    errors.privacyConsent = 'Agree to the privacy notice to apply.';
  }
  return errors;
}

/** The request body for a valid form; call {@link validateApplicationForm} first. */
export function toApplicationRequest(value: ApplicationFormValue): ApplicationRequest {
  const phone = value.phone.trim();
  const coverLetter = value.coverLetter.trim();
  return {
    fullName: value.fullName.trim(),
    email: value.email.trim(),
    phone: phone || null,
    coverLetter: coverLetter || null,
    privacyConsent: value.privacyConsent,
  };
}

const FORM_FIELDS = new Set<string>(Object.keys(EMPTY_APPLICATION));

/** Maps the API's 422 `fields` list onto the form; unknown fields are returned separately. */
export function applicationErrorsFromApi(fields: { field: string; message: string }[]): {
  errors: ApplicationFieldErrors;
  other: string[];
} {
  const errors: ApplicationFieldErrors = {};
  const other: string[] = [];
  for (const { field, message } of fields) {
    if (FORM_FIELDS.has(field)) {
      errors[field as keyof ApplicationFormValue] ??= sentence(message);
    } else {
      other.push(`${field} ${message}`);
    }
  }
  return { errors, other };
}

function sentence(message: string): string {
  const text = message.charAt(0).toUpperCase() + message.slice(1);
  return text.endsWith('.') ? text : `${text}.`;
}
