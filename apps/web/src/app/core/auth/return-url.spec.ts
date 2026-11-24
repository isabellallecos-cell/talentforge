import { describe, expect, it } from 'vitest';

import { safeReturnUrl } from './return-url';

describe('safeReturnUrl', () => {
  it('keeps in-app paths with query and fragment', () => {
    expect(safeReturnUrl('/members')).toBe('/members');
    expect(safeReturnUrl('/requisitions?status=open#top')).toBe('/requisitions?status=open#top');
  });

  it.each([null, undefined, '', 'members', 'https://evil.example/', '//evil.example/x', '/\\evil.example', 'javascript:alert(1)'])(
    'falls back to the overview for %s',
    (value) => {
      expect(safeReturnUrl(value)).toBe('/');
    },
  );

  it('rejects control characters', () => {
    expect(safeReturnUrl('/members\n//evil')).toBe('/');
  });

  it('does not send the user back to the sign-in page', () => {
    expect(safeReturnUrl('/login')).toBe('/');
    expect(safeReturnUrl('/login?returnUrl=/members')).toBe('/');
  });

  it('rejects very long values', () => {
    expect(safeReturnUrl('/' + 'a'.repeat(2048))).toBe('/');
  });
});
