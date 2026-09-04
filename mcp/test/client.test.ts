import assert from 'node:assert/strict';
import { test } from 'node:test';

import { buildUrl } from '../src/cmms/client.js';
import { mapHttpStatus } from '../src/cmms/errors.js';
import type { Operation } from '../src/openapi/operations.js';

function operation(overrides: Partial<Operation>): Operation {
  return {
    method: 'get',
    path: '/assets/{id}',
    tag: 'Assets',
    operationId: undefined,
    summary: undefined,
    description: undefined,
    deprecated: false,
    pathParams: [],
    queryParams: [],
    body: undefined,
    responseSchema: undefined,
    unsupported: undefined,
    ...overrides,
  };
}

test('path parameters are substituted and encoded', () => {
  const url = buildUrl(
    'http://api:8080',
    operation({ pathParams: [{ name: 'id', in: 'path', required: true }] }),
    { id: 42 },
  );
  assert.equal(url, 'http://api:8080/assets/42');

  const encoded = buildUrl(
    'http://api:8080',
    operation({
      path: '/assets/public/mini/{portalUUID}',
      pathParams: [{ name: 'portalUUID', in: 'path', required: true }],
    }),
    { portalUUID: 'a b/c' },
  );
  assert.equal(url.includes('..'), false);
  assert.match(encoded, /a%20b%2Fc$/);
});

test('a missing path parameter is reported, not sent as the literal template', () => {
  assert.throws(
    () =>
      buildUrl('http://api:8080', operation({ pathParams: [{ name: 'id', in: 'path', required: true }] }), {}),
    /missing required path parameter "id"/,
  );
});

test('query parameters are appended, arrays repeat, and absent optionals are skipped', () => {
  const url = buildUrl(
    'http://api:8080',
    operation({
      path: '/assets/mini',
      queryParams: [
        { name: 'locationId', in: 'query' },
        { name: 'ids', in: 'query' },
        { name: 'unused', in: 'query' },
      ],
    }),
    { locationId: 7, ids: [1, 2] },
  );
  assert.equal(url, 'http://api:8080/assets/mini?locationId=7&ids=1&ids=2');
});

test('an argument the endpoint does not know is refused instead of dropped', () => {
  // Silently dropping it turns a misplaced filter into "no results", which reads as a data
  // problem and costs an hour.
  assert.throws(
    () => buildUrl('http://api:8080', operation({ path: '/assets/mini' }), { pageSize: 10 }),
    /unknown argument\(s\) pageSize/,
  );
});

test('failures are mapped to a kind and a retry verdict', () => {
  assert.equal(mapHttpStatus(400, '{"message":"bad"}').kind, 'invalid_input');
  assert.equal(mapHttpStatus(400, '{"message":"bad"}').retryable, false);
  assert.equal(mapHttpStatus(401, '{"message":"API key has been revoked"}').kind, 'unauthenticated');
  assert.equal(mapHttpStatus(404, '').kind, 'not_found');
  assert.equal(mapHttpStatus(503, '').kind, 'temporarily_unavailable');
  assert.equal(mapHttpStatus(503, '').retryable, true);

  // A 500 carrying a message is a business refusal in this codebase (usage limits are bare
  // RuntimeExceptions), so the text has to survive and the agent must not retry.
  const limit = mapHttpStatus(500, '{"message":"You have reached the limit of assets"}');
  assert.equal(limit.kind, 'business_failure');
  assert.equal(limit.retryable, false);
  assert.match(limit.message, /limit of assets/);
  assert.equal(mapHttpStatus(500, '').kind, 'upstream_error');
});

test('the premium gate is named on the 403 it actually produces', () => {
  // ApiKeyAuthFilter answers exactly this when API_ACCESS is not entitled, which is
  // indistinguishable from a permission failure by status alone.
  const denied = mapHttpStatus(403, '{"success":false,"message":"Access denied"}');
  assert.equal(denied.kind, 'forbidden');
  assert.match(denied.message, /SELF_HOSTED_UNLOCK_PREMIUM/);

  const other = mapHttpStatus(403, '{"message":"You may not view work orders"}');
  assert.doesNotMatch(other.message, /SELF_HOSTED_UNLOCK_PREMIUM/);
});

test('a non-JSON error page still yields a message', () => {
  const gateway = mapHttpStatus(502, '<html>502 Bad Gateway</html>');
  assert.equal(gateway.kind, 'temporarily_unavailable');
  assert.match(gateway.message, /Bad Gateway/);
});

test('a body sent to an endpoint that takes none is refused', () => {
  assert.throws(
    () => buildUrl('http://api:8080', operation({ path: '/assets/mini' }), { body: { a: 1 } }),
    /unknown argument\(s\) body/,
  );
});
