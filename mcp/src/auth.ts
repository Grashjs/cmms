import type { Config } from './config.js';
import { failure, type Failure } from './cmms/errors.js';
import { keyFingerprint } from './logger.js';

/**
 * Where the API key comes from (konzept §4.2). The server holds no identity of its own in
 * either mode; it only decides which key to forward.
 *
 * - **passthrough** (default): the client presents its own key per session — an `x-api-key`
 *   header on the MCP HTTP request, or `CMMS_API_KEY` in the environment on stdio. Rights,
 *   company scope and the CMMS's own audit trail then hang off the real user.
 * - **service**: the server forwards one key from its environment. Simplest to set up, and
 *   correct only when a single trusted agent is the sole client.
 */

export interface Caller {
  apiKey: string;
  /** Short hash of the key, for the audit log. The key itself is never logged. */
  fingerprint: string;
  source: 'header' | 'environment' | 'service';
}

export type CallerResolution = { ok: true; caller: Caller } | { ok: false; failure: Failure };

/** Header maps arrive from the SDK as `Record<string, string | string[] | undefined>`. */
export type IncomingHeaders = Record<string, string | string[] | undefined> | undefined;

export function resolveCaller(config: Config, headers: IncomingHeaders): CallerResolution {
  if (config.authMode === 'service') {
    const apiKey = config.serviceApiKey;
    if (!apiKey) {
      return {
        ok: false,
        failure: failure('not_configured', 'AUTH_MODE=service but SERVICE_API_KEY is not set'),
      };
    }
    return { ok: true, caller: { apiKey, fingerprint: keyFingerprint(apiKey), source: 'service' } };
  }

  const fromHeader = headerValue(headers, 'x-api-key');
  if (fromHeader) {
    return {
      ok: true,
      caller: { apiKey: fromHeader, fingerprint: keyFingerprint(fromHeader), source: 'header' },
    };
  }

  // The environment key is the stdio transport's *only* way to carry one, and deliberately
  // not a fallback over HTTP: there, an unauthenticated caller would silently act as this
  // key's user, which on a publicly routed /mcp is a hole rather than a convenience. One
  // shared key over HTTP is what AUTH_MODE=service is for, and it says so in the config.
  if (config.stdioApiKey && config.transport === 'stdio') {
    return {
      ok: true,
      caller: {
        apiKey: config.stdioApiKey,
        fingerprint: keyFingerprint(config.stdioApiKey),
        source: 'environment',
      },
    };
  }

  return {
    ok: false,
    failure: failure(
      'unauthenticated',
      config.transport === 'stdio'
        ? 'no API key: set CMMS_API_KEY in the environment of this MCP server'
        : 'no API key: send it as an "x-api-key" header on the MCP request, or run the server with AUTH_MODE=service',
    ),
  };
}

function headerValue(headers: IncomingHeaders, name: string): string | undefined {
  if (!headers) return undefined;
  // Node lowercases incoming header names; a client that sends `X-Api-Key` must still work.
  for (const [key, value] of Object.entries(headers)) {
    if (key.toLowerCase() !== name) continue;
    const raw = Array.isArray(value) ? value[0] : value;
    const trimmed = raw?.trim();
    if (trimmed) return trimmed;
  }
  return undefined;
}
