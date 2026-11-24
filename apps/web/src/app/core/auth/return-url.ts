/**
 * Accepts a post-login target only if it is an in-app path. Anything that
 * could leave the app (absolute or protocol-relative URLs, backslash tricks)
 * or loop back to the sign-in page falls back to the overview.
 */
export function safeReturnUrl(value: string | null | undefined): string {
  if (!value || value.length > 2048) {
    return '/';
  }
  if (!value.startsWith('/') || value.startsWith('//') || value.includes('\\')) {
    return '/';
  }
  if (/[\u0000-\u001f\u007f]/.test(value)) {
    return '/';
  }
  const path = value.split(/[?#]/, 1)[0];
  if (path === '/login' || path.startsWith('/login/')) {
    return '/';
  }
  return value;
}
