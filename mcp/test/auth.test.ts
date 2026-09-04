import assert from 'node:assert/strict';
import { test } from 'node:test';

import { resolveCaller } from '../src/auth.js';
import { keyFingerprint } from '../src/logger.js';
import { RateLimiter } from '../src/ratelimit.js';
import { testConfig } from './helpers.js';

test('passthrough takes the key from the request header, whatever its casing', () => {
  const config = testConfig();
  const lower = resolveCaller(config, { 'x-api-key': 'abc' });
  assert.equal(lower.ok, true);
  assert.equal(lower.ok && lower.caller.source, 'header');
  assert.equal(lower.ok && lower.caller.apiKey, 'abc');

  const upper = resolveCaller(config, { 'X-Api-Key': ' abc ' });
  assert.equal(upper.ok && upper.caller.apiKey, 'abc');

  const repeated = resolveCaller(config, { 'x-api-key': ['first', 'second'] });
  assert.equal(repeated.ok && repeated.caller.apiKey, 'first');
});

test('without a key nothing is called, and the message says how to supply one', () => {
  const http = resolveCaller(testConfig(), {});
  assert.equal(http.ok, false);
  assert.equal(!http.ok && http.failure.kind, 'unauthenticated');
  assert.match(!http.ok ? http.failure.message : '', /x-api-key/);

  const stdio = resolveCaller(testConfig({ MCP_TRANSPORT: 'stdio' }), undefined);
  assert.match(!stdio.ok ? stdio.failure.message : '', /CMMS_API_KEY/);
});

test('stdio falls back to the environment key', () => {
  const config = testConfig({ MCP_TRANSPORT: 'stdio', CMMS_API_KEY: 'env-key' });
  const resolved = resolveCaller(config, undefined);
  assert.equal(resolved.ok && resolved.caller.source, 'environment');
});

test('service mode ignores a client-supplied key', () => {
  const config = testConfig({ AUTH_MODE: 'service', SERVICE_API_KEY: 'service-key' });
  const resolved = resolveCaller(config, { 'x-api-key': 'someone-elses-key' });
  assert.equal(resolved.ok && resolved.caller.apiKey, 'service-key');
  assert.equal(resolved.ok && resolved.caller.source, 'service');
});

test('service mode without a key refuses to start', () => {
  assert.throws(() => testConfig({ AUTH_MODE: 'service' }), /requires SERVICE_API_KEY/);
});

test('the fingerprint identifies a key without revealing it', () => {
  const print = keyFingerprint('super-secret-key');
  assert.equal(print.length, 12);
  assert.match(print, /^[0-9a-f]+$/);
  assert.equal(print, keyFingerprint('super-secret-key'));
  assert.notEqual(print, keyFingerprint('another-key'));
  assert.equal(print.includes('secret'), false);
});

test('the rate limiter allows a burst, then refuses, then refills', () => {
  let now = 0;
  const limiter = new RateLimiter({ perMinute: 60, burst: 3, now: () => now });

  assert.equal(limiter.take('key').allowed, true);
  assert.equal(limiter.take('key').allowed, true);
  assert.equal(limiter.take('key').allowed, true);

  const refused = limiter.take('key');
  assert.equal(refused.allowed, false);
  assert.ok(refused.retryAfterSeconds >= 1);

  // A second key has its own bucket: one agent in a loop must not lock out another client.
  assert.equal(limiter.take('other').allowed, true);

  now += 1000; // 60/minute is one token per second
  assert.equal(limiter.take('key').allowed, true);
});

test('RATE_LIMIT=0 disables limiting', () => {
  const limiter = new RateLimiter({ perMinute: 0, burst: 1 });
  for (let index = 0; index < 100; index += 1) {
    assert.equal(limiter.take('key').allowed, true);
  }
});

test('CMMS_API_KEY is not a fallback over HTTP', () => {
  // Over HTTP an unauthenticated caller would otherwise silently act as this key's user,
  // and /mcp is publicly routed. One shared key over HTTP is AUTH_MODE=service.
  const config = testConfig({ MCP_TRANSPORT: 'http', CMMS_API_KEY: 'env-key' });
  const resolved = resolveCaller(config, {});
  assert.equal(resolved.ok, false);
  assert.equal(!resolved.ok && resolved.failure.kind, 'unauthenticated');
});
