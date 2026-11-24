/** Error envelope returned by every API failure (`ApiError` on the server). */
export interface ApiErrorBody {
  code: string;
  message: string;
  fields?: { field: string; message: string }[];
}

export interface ApiFailure {
  status: number;
  code: string;
  message: string;
  fields: { field: string; message: string }[];
}

const FALLBACK_MESSAGES: Record<number, string> = {
  0: 'The server could not be reached. Check your connection and try again.',
  401: 'Sign in to continue.',
  403: 'You do not have permission for this action.',
  404: 'That page or record does not exist.',
};

function isApiErrorBody(body: unknown): body is ApiErrorBody {
  return (
    typeof body === 'object' &&
    body !== null &&
    typeof (body as ApiErrorBody).code === 'string' &&
    typeof (body as ApiErrorBody).message === 'string'
  );
}

/**
 * Normalizes an `HttpErrorResponse`-like value into something the UI can
 * show. Only the server's envelope message is displayed; raw bodies (HTML
 * error pages, stack traces from a proxy) never reach the screen.
 */
export function toApiFailure(error: { status?: number; error?: unknown }): ApiFailure {
  const status = typeof error.status === 'number' ? error.status : 0;
  if (isApiErrorBody(error.error)) {
    return {
      status,
      code: error.error.code,
      message: error.error.message,
      fields: Array.isArray(error.error.fields) ? error.error.fields : [],
    };
  }
  return {
    status,
    code: status === 0 ? 'network_error' : 'unexpected_error',
    message: FALLBACK_MESSAGES[status] ?? 'Something went wrong. Try again in a moment.',
    fields: [],
  };
}
