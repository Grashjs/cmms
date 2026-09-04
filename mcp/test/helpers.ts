import { readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { loadConfig, type Config } from '../src/config.js';
import { createLogger, type Logger } from '../src/logger.js';
import type { OpenApiDocument } from '../src/openapi/types.js';

const here = dirname(fileURLToPath(import.meta.url));

/**
 * The real document as served by the deployed instance, saved verbatim. Tests that assert
 * on tool names and counts run against this rather than a hand-written stub, because the
 * things that break here — springdoc's generated operationIds, POST-based search, missing
 * descriptions — are properties of the real document and no stub would reproduce them.
 */
export function realDocument(): OpenApiDocument {
  // From the compiled test in dist/test, the fixture sits next to the source tree.
  for (const candidate of [
    join(here, 'fixtures', 'api-docs.json'),
    join(here, '..', '..', 'test', 'fixtures', 'api-docs.json'),
  ]) {
    try {
      return JSON.parse(readFileSync(candidate, 'utf8')) as OpenApiDocument;
    } catch {
      continue;
    }
  }
  throw new Error('test/fixtures/api-docs.json not found');
}

export function testConfig(overrides: Record<string, string> = {}): Config {
  return loadConfig({
    CMMS_BASE_URL: 'http://api:8080',
    ...overrides,
  } as NodeJS.ProcessEnv);
}

/** A logger that keeps its lines instead of writing them, so a test can assert on them. */
export function recordingLogger(): { logger: Logger; lines: string[] } {
  const lines: string[] = [];
  const logger = createLogger({ logLevel: 'debug' }, { write: (line) => lines.push(line) });
  return { logger, lines };
}
