import { createHash } from 'node:crypto';

import type { Config } from './config.js';

/**
 * Everything is written to **stderr**, never stdout. On the stdio transport stdout carries
 * the JSON-RPC framing, and a single stray log line there corrupts the protocol stream for
 * the client. Under Docker stderr is captured just the same, so nothing is lost.
 */

const LEVELS = { debug: 10, info: 20, warn: 30, error: 40 } as const;

export type Level = keyof typeof LEVELS;

export interface AuditEntry {
  /** `tool_call`, `tool_denied`, `spec_loaded`, … */
  event: string;
  tool?: string;
  method?: string;
  path?: string;
  /** HTTP status the CMMS answered with, when a call was made. */
  status?: number;
  /** Mapped failure kind, see cmms/errors.ts. */
  kind?: string;
  durationMs?: number;
  /** Truncated hash of the API key. Never the key itself. */
  key?: string;
  sessionId?: string;
  message?: string;
  [extra: string]: unknown;
}

export interface Logger {
  debug(message: string, fields?: Record<string, unknown>): void;
  info(message: string, fields?: Record<string, unknown>): void;
  warn(message: string, fields?: Record<string, unknown>): void;
  error(message: string, fields?: Record<string, unknown>): void;
  /**
   * One line per tool invocation: who (hashed key), what (tool, endpoint), outcome
   * (status, kind), how long. Never the arguments and never the response body — the audit
   * answers "who did what to which endpoint", not "with which data".
   */
  audit(entry: AuditEntry): void;
}

/** Anything that takes a line. `process.stderr` in production, a buffer in tests. */
export interface LogSink {
  write(line: string): unknown;
}

export function createLogger(
  config: Pick<Config, 'logLevel'>,
  sink: LogSink = process.stderr,
): Logger {
  const threshold = LEVELS[config.logLevel] ?? LEVELS.info;

  const write = (level: Level, message: string, fields?: Record<string, unknown>): void => {
    if (LEVELS[level] < threshold) return;
    sink.write(`${JSON.stringify({ ts: new Date().toISOString(), level, message, ...fields })}\n`);
  };

  return {
    debug: (message, fields) => write('debug', message, fields),
    info: (message, fields) => write('info', message, fields),
    warn: (message, fields) => write('warn', message, fields),
    error: (message, fields) => write('error', message, fields),
    audit: (entry) => {
      // Audit lines are the record of who did what and are not subject to LOG_LEVEL.
      sink.write(`${JSON.stringify({ ts: new Date().toISOString(), level: 'audit', ...entry })}\n`);
    },
  };
}

/**
 * A stable, short, non-reversible label for an API key, so consecutive calls can be
 * attributed to the same caller in the audit log without the key ever being written down.
 * The CMMS stores keys the same way (`Helper.hashKey`, SHA-256), just untruncated.
 */
export function keyFingerprint(apiKey: string): string {
  return createHash('sha256').update(apiKey).digest('hex').slice(0, 12);
}
