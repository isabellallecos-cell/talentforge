/** Permission keys from the server's `Permission` enum. */
export type PermissionKey =
  | 'members:view'
  | 'members:manage'
  | 'requisitions:view'
  | 'requisitions:create'
  | 'requisitions:approve'
  | 'candidates:view'
  | 'candidates:manage'
  | 'interviews:view-assigned'
  | 'scorecards:submit';

export interface NavItem {
  label: string;
  path: string;
  /** Shown when the member has any of these; empty = everyone. */
  anyOf: PermissionKey[];
}

/**
 * Sections of the app shell. Items for screens that are not built yet stay
 * out of this list so the navigation never links to an empty page.
 */
export const NAV_ITEMS: NavItem[] = [
  { label: 'Overview', path: '/', anyOf: [] },
  { label: 'Requisitions', path: '/requisitions', anyOf: ['requisitions:view'] },
  { label: 'Members', path: '/members', anyOf: ['members:view'] },
];

/** Navigation hiding is a convenience; the API checks every request. */
export function visibleNavItems(items: NavItem[], permissions: ReadonlySet<string>): NavItem[] {
  return items.filter((item) => item.anyOf.length === 0 || item.anyOf.some((key) => permissions.has(key)));
}

/**
 * Picks the organization to show: the remembered one if the user is still a
 * member, otherwise the first membership (alphabetical by name).
 */
export function pickOrganization(
  memberships: { organizationId: string; organizationName: string }[],
  remembered: string | null,
): string | null {
  if (memberships.length === 0) {
    return null;
  }
  if (remembered && memberships.some((m) => m.organizationId === remembered)) {
    return remembered;
  }
  return [...memberships].sort((a, b) => a.organizationName.localeCompare(b.organizationName))[0].organizationId;
}
