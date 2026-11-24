import { describe, expect, it } from 'vitest';

import { toApiFailure } from './api-error';

describe('toApiFailure', () => {
  it('uses the server envelope', () => {
    expect(
      toApiFailure({ status: 401, error: { code: 'invalid_credentials', message: 'Email or password is incorrect.' } }),
    ).toEqual({ status: 401, code: 'invalid_credentials', message: 'Email or password is incorrect.', fields: [] });
  });

  it('keeps field issues', () => {
    const failure = toApiFailure({
      status: 422,
      error: { code: 'validation_failed', message: 'Check the form.', fields: [{ field: 'email', message: 'must be a well-formed email address' }] },
    });
    expect(failure.fields).toEqual([{ field: 'email', message: 'must be a well-formed email address' }]);
  });

  it('reports network failures', () => {
    expect(toApiFailure({ status: 0, error: new Error('offline') })).toMatchObject({ code: 'network_error' });
  });

  it('never shows a raw non-envelope body', () => {
    const failure = toApiFailure({ status: 502, error: '<html>Bad Gateway</html>' });
    expect(failure.code).toBe('unexpected_error');
    expect(failure.message).not.toContain('<html>');
  });

  it('has readable fallbacks for common statuses', () => {
    expect(toApiFailure({ status: 403 }).message).toBe('You do not have permission for this action.');
    expect(toApiFailure({ status: 404, error: null }).code).toBe('unexpected_error');
  });
});
