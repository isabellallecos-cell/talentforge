import { describe, expect, it } from 'vitest';

import { NAV_ITEMS, NavItem, pickOrganization, visibleNavItems } from './navigation';

// Permission sets as returned by GET /v1/organizations/{id}/access (see docs/security/role-permissions.md).
const RECRUITER = new Set([
  'candidates:manage',
  'candidates:view',
  'interviews:view-assigned',
  'members:view',
  'requisitions:create',
  'requisitions:view',
]);
const HIRING_MANAGER = new Set([
  'candidates:view',
  'interviews:view-assigned',
  'requisitions:approve',
  'requisitions:create',
  'requisitions:view',
  'scorecards:submit',
]);
const INTERVIEWER = new Set(['interviews:view-assigned', 'scorecards:submit']);

describe('visibleNavItems', () => {
  it('shows requisitions and members to recruiters but not interviewers', () => {
    expect(visibleNavItems(NAV_ITEMS, RECRUITER).map((i) => i.path)).toEqual(['/', '/requisitions', '/members']);
    expect(visibleNavItems(NAV_ITEMS, INTERVIEWER).map((i) => i.path)).toEqual(['/']);
  });

  it('shows requisitions to hiring managers, who cannot list members', () => {
    expect(visibleNavItems(NAV_ITEMS, HIRING_MANAGER).map((i) => i.path)).toEqual(['/', '/requisitions']);
  });

  it('shows only open items when permissions are unknown', () => {
    expect(visibleNavItems(NAV_ITEMS, new Set()).map((i) => i.path)).toEqual(['/']);
  });

  it('matches any of several permissions', () => {
    const items: NavItem[] = [{ label: 'Interviews', path: '/interviews', anyOf: ['interviews:view-assigned', 'scorecards:submit'] }];
    expect(visibleNavItems(items, new Set(['scorecards:submit']))).toHaveLength(1);
    expect(visibleNavItems(items, new Set(['members:view']))).toHaveLength(0);
  });
});

describe('pickOrganization', () => {
  const memberships = [
    { organizationId: 'b', organizationName: 'Northwind' },
    { organizationId: 'a', organizationName: 'Acme' },
  ];

  it('keeps the remembered organization while the user is a member', () => {
    expect(pickOrganization(memberships, 'b')).toBe('b');
  });

  it('falls back to the first by name when the remembered one is gone', () => {
    expect(pickOrganization(memberships, 'removed')).toBe('a');
    expect(pickOrganization(memberships, null)).toBe('a');
  });

  it('returns null without memberships', () => {
    expect(pickOrganization([], 'a')).toBeNull();
  });
});
