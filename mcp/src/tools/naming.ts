import type { Operation } from '../openapi/operations.js';

/**
 * Tool names are derived from **method plus path**, never from `operationId`.
 *
 * This is not a style preference. springdoc names operations after the Java method and
 * appends a scan-order counter when names collide, so this document calls
 * `POST /assets/search` → `search_15`, `GET /assets/{id}` → `getById_39`,
 * `PATCH /assets/{id}` → `patch_40`. Two problems, both fatal for a tool contract:
 *
 * 1. **Unusable.** `search_15` tells a model nothing about what it searches.
 * 2. **Unstable.** The counter depends on how many same-named methods were scanned before
 *    it. Adding one controller upstream renumbers unrelated tools, silently breaking every
 *    client allowlist and every saved agent configuration.
 *
 * Method + path is unique by construction (a path item cannot repeat a method), readable,
 * and changes only when the endpoint itself changes — at which point breaking is correct.
 */

export function toolNameFor(operation: Operation): string {
  const segments = operation.path
    .split('/')
    .filter((segment) => segment.length > 0)
    .map((segment) =>
      segment.startsWith('{') && segment.endsWith('}')
        ? `by_${segment.slice(1, -1)}`
        : segment,
    );

  return sanitise([operation.method, ...segments].join('_'));
}

function sanitise(name: string): string {
  return name
    .replace(/[^a-zA-Z0-9_]+/g, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '');
}

/**
 * Matches a tool name against a glob. Only `*` is supported (any run of characters), which
 * covers the shapes an allowlist actually needs: `get_*`, `*_assets_*`, `delete_*`.
 */
export function matchesGlob(name: string, pattern: string): boolean {
  if (pattern === '*') return true;
  const expression = pattern
    .split('*')
    .map((part) => part.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
    .join('.*');
  return new RegExp(`^${expression}$`).test(name);
}

export function matchesAnyGlob(name: string, patterns: string[]): boolean {
  return patterns.some((pattern) => matchesGlob(name, pattern));
}
