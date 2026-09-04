/**
 * Failure translation (konzept §4.5).
 *
 * Without this every failure reaches the agent as "something went wrong", and it retries a
 * 403 forever while giving up on a 503 that would have worked a second later. The CMMS has
 * distinguishable failure states and they carry different instructions:
 *
 * | CMMS            | means                                        | agent should      |
 * |-----------------|----------------------------------------------|-------------------|
 * | 400 / 422       | validation                                   | correct the input |
 * | 401             | the key is missing, revoked, expired, or the  | stop, ask a human |
 * |                 | user is disabled                             |                   |
 * | 403             | role or permission missing — or API access is | stop, ask a human |
 * |                 | not unlocked at all (see below)              |                   |
 * | 404             | no such record                               | stop or search    |
 * | 429             | rate limited                                 | back off, retry   |
 * | 500 with a text | a business failure, e.g. a usage limit,       | pass the text on, |
 * |                 | raised as a bare RuntimeException            | do not retry      |
 * | 502/503/504     | database or service not ready                | retry, sparingly  |
 *
 * The 403 line has a trap worth naming: `ApiKeyAuthFilter` answers `403 {"message":"Access
 * denied"}` when the license entitlement or the plan feature `API_ACCESS` is missing, which
 * on a self-hosted instance means `SELF_HOSTED_UNLOCK_PREMIUM` is not `true` (CLAUDE.md).
 * That is indistinguishable from an ordinary permission failure by status alone, so the hint
 * is attached to the message instead of being guessed at by the agent.
 */

export type FailureKind =
  | 'invalid_input'
  | 'unauthenticated'
  | 'forbidden'
  | 'not_found'
  | 'rate_limited'
  | 'business_failure'
  | 'temporarily_unavailable'
  | 'upstream_error'
  | 'transport_failure'
  | 'not_configured';

export interface Failure {
  kind: FailureKind;
  /** HTTP status the CMMS answered with, when there was a response at all. */
  status?: number;
  message: string;
  /** Whether an agent may sensibly try the same call again. */
  retryable: boolean;
  /** What the agent should do instead of guessing. */
  advice: string;
}

const ADVICE: Record<FailureKind, string> = {
  invalid_input: 'Correct the arguments and call again.',
  unauthenticated:
    'The API key was rejected. Do not retry; a person has to supply a valid key.',
  forbidden:
    'The user this API key belongs to may not do this. Do not retry; a person has to grant the permission or supply a different key.',
  not_found: 'The record does not exist. Search for it instead of retrying the same id.',
  rate_limited: 'Wait before calling again, and make fewer calls.',
  business_failure:
    'The CMMS refused this on business grounds. The message is the reason; retrying will fail the same way.',
  temporarily_unavailable:
    'The CMMS is not ready (it needs tens of seconds after a restart). Retry once after a pause.',
  upstream_error: 'Unexpected failure in the CMMS. Retry once, then report it.',
  transport_failure: 'The CMMS could not be reached at all. Retry once, then report it.',
  not_configured:
    'This MCP server is missing configuration and cannot make the call. A person has to fix the deployment.',
};

export function failure(
  kind: FailureKind,
  message: string,
  status?: number,
  retryable?: boolean,
): Failure {
  const defaultRetryable = kind === 'temporarily_unavailable' || kind === 'rate_limited' || kind === 'transport_failure';
  return {
    kind,
    ...(status === undefined ? {} : { status }),
    message,
    retryable: retryable ?? defaultRetryable,
    advice: ADVICE[kind],
  };
}

export function mapHttpStatus(status: number, body: string): Failure {
  const message = extractMessage(body) ?? `${status} from the CMMS`;

  if (status === 400 || status === 422) return failure('invalid_input', message, status);
  if (status === 401) return failure('unauthenticated', message, status);
  if (status === 403) {
    const hint =
      /access denied/i.test(message)
        ? `${message} — if this is a self-hosted instance, check that SELF_HOSTED_UNLOCK_PREMIUM=true is set on the api service, because ApiKeyAuthFilter answers exactly this when API_ACCESS is not entitled`
        : message;
    return failure('forbidden', hint, status);
  }
  if (status === 404) return failure('not_found', message, status);
  if (status === 429) return failure('rate_limited', message, status);
  if (status === 502 || status === 503 || status === 504) {
    return failure('temporarily_unavailable', message, status);
  }
  if (status === 500) {
    // A 500 carrying a message is a business refusal in this codebase: usage limits and
    // similar checks are thrown as bare RuntimeExceptions rather than typed failures.
    return extractMessage(body)
      ? failure('business_failure', message, status)
      : failure('upstream_error', message, status);
  }
  if (status >= 500) return failure('upstream_error', message, status);
  return failure('upstream_error', message, status);
}

/** The CMMS answers failures as `{"success": false, "message": "..."}`. */
function extractMessage(body: string): string | undefined {
  const trimmed = body.trim();
  if (trimmed.length === 0) return undefined;
  try {
    const parsed = JSON.parse(trimmed) as Record<string, unknown>;
    for (const field of ['message', 'error', 'detail']) {
      const value = parsed[field];
      if (typeof value === 'string' && value.trim().length > 0) return value.trim();
    }
  } catch {
    // Not JSON — a proxy error page, for instance. The text is still the best answer.
  }
  return trimmed.slice(0, 500);
}
