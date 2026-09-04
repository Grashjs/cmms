import type { Config } from '../config.js';
import type { Operation } from '../openapi/operations.js';
import { failure, mapHttpStatus, type Failure } from './errors.js';

/**
 * The whole proxy, in one function: take the tool arguments, put them where the HTTP
 * request wants them, add the caller's API key, and hand the answer back. No caching, no
 * state between calls, no interpretation of what the CMMS said beyond translating how it
 * failed (konzept §1, §4.1).
 */

export interface CallResult {
  ok: boolean;
  status: number;
  /** Parsed JSON when the CMMS answered JSON, the raw text otherwise. */
  body: unknown;
  /** Set when `ok` is false. */
  failure?: Failure;
  /** True when the body was cut to MAX_RESPONSE_CHARS. */
  truncated: boolean;
  durationMs: number;
}

export interface CallOptions {
  apiKey: string;
  signal?: AbortSignal;
}

export async function callOperation(
  config: Config,
  operation: Operation,
  args: Record<string, unknown>,
  options: CallOptions,
): Promise<CallResult> {
  const started = Date.now();

  let url: string;
  try {
    url = buildUrl(config.cmmsBaseUrl, operation, args);
  } catch (error) {
    return {
      ok: false,
      status: 0,
      body: null,
      failure: failure('invalid_input', error instanceof Error ? error.message : String(error)),
      truncated: false,
      durationMs: Date.now() - started,
    };
  }

  const headers: Record<string, string> = {
    // The one and only credential. The CMMS hashes it, loads the owning user and applies
    // that user's role, permissions and company (ApiKeyAuthFilter) — which is why this
    // server needs no identity of its own and can never exceed the key it was handed.
    'x-api-key': options.apiKey,
    accept: 'application/json',
  };

  const body = args.body;
  const hasBody = operation.body !== undefined && body !== undefined;
  if (hasBody) headers['content-type'] = 'application/json';

  const timeout = AbortSignal.timeout(config.requestTimeoutMs);
  const signal = options.signal ? AbortSignal.any([options.signal, timeout]) : timeout;

  let response: Response;
  try {
    response = await fetch(url, {
      method: operation.method.toUpperCase(),
      headers,
      ...(hasBody ? { body: JSON.stringify(body) } : {}),
      signal,
    });
  } catch (error) {
    const message = error instanceof Error ? error.message : String(error);
    const aborted = error instanceof Error && error.name === 'TimeoutError';
    return {
      ok: false,
      status: 0,
      body: null,
      failure: failure(
        aborted ? 'temporarily_unavailable' : 'transport_failure',
        aborted ? `the CMMS did not answer within ${config.requestTimeoutMs} ms` : message,
      ),
      truncated: false,
      durationMs: Date.now() - started,
    };
  }

  const text = await response.text();
  const durationMs = Date.now() - started;

  if (!response.ok) {
    return {
      ok: false,
      status: response.status,
      body: text,
      failure: mapHttpStatus(response.status, text),
      truncated: false,
      durationMs,
    };
  }

  const truncated = text.length > config.maxResponseChars;
  const payload = truncated ? text.slice(0, config.maxResponseChars) : text;

  return {
    ok: true,
    status: response.status,
    // Truncated text can no longer be valid JSON, so it stays text and says so.
    body: truncated ? payload : parseJson(payload),
    truncated,
    durationMs,
  };
}

function parseJson(text: string): unknown {
  if (text.trim().length === 0) return null;
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

/**
 * Path parameters are substituted into the template, query parameters are appended, and
 * everything else in `args` other than `body` is rejected rather than silently dropped: a
 * misplaced argument that disappears turns into an unexplained "no results".
 */
export function buildUrl(
  baseUrl: string,
  operation: Operation,
  args: Record<string, unknown>,
): string {
  let path = operation.path;

  for (const parameter of operation.pathParams) {
    const value = args[parameter.name];
    if (value === undefined || value === null || value === '') {
      throw new Error(`missing required path parameter "${parameter.name}"`);
    }
    path = path.replace(`{${parameter.name}}`, encodeURIComponent(String(value)));
  }

  const url = new URL(baseUrl + path);

  const known = new Set<string>([
    ...operation.pathParams.map((parameter) => parameter.name),
    ...operation.queryParams.map((parameter) => parameter.name),
    // Only where the endpoint has one: a `body` sent to an endpoint that takes none would
    // otherwise be dropped without a word, and the call would look like it had worked.
    ...(operation.body ? ['body'] : []),
  ]);

  for (const parameter of operation.queryParams) {
    const value = args[parameter.name];
    if (value === undefined || value === null) {
      if (parameter.required) {
        throw new Error(`missing required query parameter "${parameter.name}"`);
      }
      continue;
    }
    if (Array.isArray(value)) {
      // Spring binds repeated parameters to a collection.
      for (const entry of value) url.searchParams.append(parameter.name, String(entry));
    } else {
      url.searchParams.set(parameter.name, String(value));
    }
  }

  const unexpected = Object.keys(args).filter((key) => !known.has(key));
  if (unexpected.length > 0) {
    throw new Error(
      `unknown argument(s) ${unexpected.join(', ')} — this endpoint accepts ${[...known].join(', ')}`,
    );
  }

  return url.toString();
}
