import { readFile } from 'node:fs/promises';

import type { Config } from '../config.js';
import type { Logger } from '../logger.js';
import type { OpenApiDocument } from './types.js';

/**
 * The OpenAPI document is loaded at runtime rather than baked into generated code. That is
 * what makes "a new endpoint upstream becomes a tool" true without a code-generation step
 * in between, and it is the reason readiness means "the document is loaded" (konzept §6).
 *
 * The URL matters: `springdoc.enable-default-api-docs` is false in `application.yml`, so
 * `/v3/api-docs` answers **404** and only the named group `/v3/api-docs/atlas-cmms` serves
 * the document. Getting this wrong looks exactly like the API being down.
 */

export interface SpecSource {
  document: OpenApiDocument;
  /** Where it came from, for logs and the health endpoint. */
  origin: string;
  loadedAt: Date;
}

export async function loadSpec(config: Config, logger: Logger): Promise<SpecSource> {
  try {
    const document = await fetchSpec(config);
    return { document, origin: config.specUrl, loadedAt: new Date() };
  } catch (error) {
    if (!config.specFile) throw error;
    // A local copy is a deliberate fallback, not a cache: it lets the stdio transport run
    // against a saved document and keeps a restart from failing while the API is booting.
    logger.warn('could not fetch the OpenAPI document, falling back to SPEC_FILE', {
      specUrl: config.specUrl,
      specFile: config.specFile,
      error: error instanceof Error ? error.message : String(error),
    });
    const raw = await readFile(config.specFile, 'utf8');
    return {
      document: JSON.parse(raw) as OpenApiDocument,
      origin: config.specFile,
      loadedAt: new Date(),
    };
  }
}

async function fetchSpec(config: Config): Promise<OpenApiDocument> {
  const response = await fetch(config.specUrl, {
    headers: { accept: 'application/json' },
    signal: AbortSignal.timeout(config.requestTimeoutMs),
  });
  if (!response.ok) {
    const hint =
      response.status === 404
        ? ' — /v3/api-docs itself is disabled on this API (enable-default-api-docs: false); the document lives at the group URL /v3/api-docs/atlas-cmms'
        : '';
    throw new Error(`GET ${config.specUrl} answered ${response.status}${hint}`);
  }
  const document = (await response.json()) as OpenApiDocument;
  if (!document.paths || Object.keys(document.paths).length === 0) {
    throw new Error(`GET ${config.specUrl} returned a document without paths`);
  }
  return document;
}

/**
 * Retries with a bounded backoff. The API needs tens of seconds to finish Liquibase,
 * Hibernate and Quartz (CLAUDE.md), so a container that starts alongside it has to wait
 * rather than crash — but it must not wait silently forever either.
 */
export async function loadSpecWithRetry(
  config: Config,
  logger: Logger,
  attempts = 30,
  delayMs = 5000,
): Promise<SpecSource> {
  let lastError: unknown;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const source = await loadSpec(config, logger);
      logger.info('OpenAPI document loaded', {
        origin: source.origin,
        openapi: source.document.openapi,
        paths: Object.keys(source.document.paths ?? {}).length,
        attempt,
      });
      return source;
    } catch (error) {
      lastError = error;
      logger.warn('OpenAPI document not available yet', {
        attempt,
        attempts,
        error: error instanceof Error ? error.message : String(error),
      });
      if (attempt < attempts) await sleep(delayMs);
    }
  }
  throw lastError instanceof Error ? lastError : new Error(String(lastError));
}

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, ms).unref?.();
  });
}
