import assert from 'node:assert/strict';
import { test } from 'node:test';

import { extractOperations } from '../src/openapi/operations.js';
import { matchesGlob, toolNameFor } from '../src/tools/naming.js';
import { realDocument } from './helpers.js';

const operations = extractOperations(realDocument());

function nameOf(method: string, path: string): string {
  const operation = operations.find((entry) => entry.method === method && entry.path === path);
  assert.ok(operation, `${method} ${path} missing from the fixture`);
  return toolNameFor(operation);
}

test('names come from method and path', () => {
  assert.equal(nameOf('post', '/assets/search'), 'post_assets_search');
  assert.equal(nameOf('get', '/assets/{id}'), 'get_assets_by_id');
  assert.equal(nameOf('patch', '/assets/{id}'), 'patch_assets_by_id');
  assert.equal(nameOf('delete', '/assets/{id}'), 'delete_assets_by_id');
  assert.equal(nameOf('get', '/work-orders/asset/{id}'), 'get_work_orders_asset_by_id');
});

test('the generated name does not inherit springdoc\'s counter suffix', () => {
  // The document really does call these search_15 / getById_39 / patch_40, and the number
  // shifts whenever an unrelated controller is added upstream. A tool name carrying it
  // would break every client allowlist on a sync that changed nothing about assets.
  const search = operations.find((o) => o.method === 'post' && o.path === '/assets/search');
  assert.equal(search?.operationId, 'search_15');
  assert.doesNotMatch(toolNameFor(search!), /\d/);
});

test('every operation in the real document yields a unique, client-safe name', () => {
  const names = new Map<string, string>();
  for (const operation of operations) {
    const name = toolNameFor(operation);
    // Letters, digits and underscores only. Mixed case is allowed and kept as written,
    // because some paths carry camelCase segments (`/analytics/.../counts/completedBy`) and
    // a name that mirrors its path is easier to trace back than one that has been folded.
    assert.match(name, /^[a-z][A-Za-z0-9_]*$/, `${name} is not a plain identifier`);
    assert.ok(name.length <= 64, `${name} is ${name.length} characters, over the 64 limit`);
    const clash = names.get(name);
    assert.equal(clash, undefined, `${name} produced by both ${clash} and ${operation.path}`);
    names.set(name, `${operation.method} ${operation.path}`);
  }
  assert.equal(names.size, operations.length);
});

test('globs match the shapes an allowlist uses', () => {
  assert.ok(matchesGlob('get_assets_by_id', 'get_*'));
  assert.ok(matchesGlob('get_assets_by_id', '*_assets_*'));
  assert.ok(matchesGlob('anything', '*'));
  assert.ok(!matchesGlob('post_assets', 'get_*'));
  // A glob is not a regular expression: the dot is literal, not "any character".
  assert.ok(!matchesGlob('get_assets', 'get.assets'));
});
