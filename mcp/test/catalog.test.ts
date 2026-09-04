import assert from 'node:assert/strict';
import { test } from 'node:test';

import { extractOperations } from '../src/openapi/operations.js';
import { classify, isBlocked } from '../src/tools/describe.js';
import { buildCatalog } from '../src/tools/registry.js';
import { realDocument, recordingLogger, testConfig } from './helpers.js';

const document = realDocument();

function catalogFor(env: Record<string, string> = {}) {
  const { logger } = recordingLogger();
  return buildCatalog(document, testConfig(env), logger);
}

test('search endpoints count as reading even though they are POST', () => {
  const operations = extractOperations(document);
  const find = (method: string, path: string) => {
    const operation = operations.find((entry) => entry.method === method && entry.path === path);
    assert.ok(operation, `${method} ${path} missing`);
    return operation;
  };

  // This is the whole reason classification is not derived from the HTTP method: get it
  // wrong and PROFILE=readonly cannot list anything at all.
  assert.equal(classify(find('post', '/assets/search')).readOnly, true);
  assert.equal(classify(find('post', '/work-orders/search')).readOnly, true);
  assert.equal(classify(find('post', '/analytics/assets/overview')).readOnly, true);
  assert.equal(classify(find('post', '/work-orders/events')).readOnly, true);
  assert.equal(classify(find('post', '/readings/meter/{id}/histogram')).readOnly, true);

  assert.equal(classify(find('post', '/assets')).readOnly, false);
  assert.equal(classify(find('patch', '/assets/{id}')).readOnly, false);
  assert.equal(classify(find('delete', '/assets/{id}')).readOnly, false);
  assert.equal(classify(find('delete', '/assets/{id}')).destructive, true);
  assert.equal(classify(find('post', '/assets')).idempotent, false);
});

test('credential, session and billing endpoints are never offered', () => {
  const operations = extractOperations(document);
  const blocked = operations.filter(isBlocked).map((o) => o.path);
  assert.ok(blocked.includes('/auth/signin'));
  assert.ok(blocked.includes('/auth/signup'));
  assert.ok(blocked.includes('/auth/updatepwd'));
  assert.ok(blocked.includes('/subscriptions/upgrade'));
  assert.ok(!blocked.includes('/assets/search'));

  const catalog = catalogFor({ PROFILE: 'full' });
  assert.equal(
    catalog.all.find((tool) => tool.operation.path.startsWith('/auth/')),
    undefined,
    'an /auth/ endpoint reached the catalogue',
  );
});

test('file upload endpoints are excluded with a stated reason, not silently dropped', () => {
  const catalog = catalogFor({ PROFILE: 'full' });
  const upload = catalog.excluded.find((entry) => entry.path === '/files/upload');
  assert.ok(upload, '/files/upload should be excluded');
  assert.match(upload.reason, /binary/);
});

test('curated names replace the generated ones, and only once', () => {
  const catalog = catalogFor({ PROFILE: 'full' });
  const byName = new Map(catalog.all.map((tool) => [tool.name, tool]));

  const search = byName.get('search_assets');
  assert.ok(search, 'search_assets missing');
  assert.equal(search.operation.path, '/assets/search');
  assert.equal(search.curated, true);
  // The generated name must be gone: two ways to call one endpoint is two chances to
  // choose wrong.
  assert.equal(byName.get('post_assets_search'), undefined);

  const names = catalog.all.map((tool) => tool.name);
  assert.equal(new Set(names).size, names.length, 'duplicate tool name in the catalogue');
});

test('every curated entry points at an endpoint that exists', async () => {
  const { CURATED_TOOLS } = await import('../src/tools/curated.js');
  const operations = extractOperations(document);
  for (const key of Object.keys(CURATED_TOOLS)) {
    const [method, path] = key.split(' ');
    const found = operations.some(
      (operation) => operation.method === method!.toLowerCase() && operation.path === path,
    );
    assert.ok(found, `curated entry ${key} matches no operation in the document`);
  }
});

test('readonly is the default profile and it can still search', () => {
  const catalog = catalogFor();
  assert.equal(catalog.profile.name, 'readonly');
  assert.ok(catalog.byName.has('search_assets'));
  assert.ok(catalog.byName.has('search_work_orders'));
  assert.ok(catalog.byName.has('get_asset'));
  assert.equal(catalog.byName.has('change_work_order_status'), false);
  assert.ok(
    catalog.visible.every((tool) => tool.classification.readOnly),
    'a writing tool is visible under PROFILE=readonly',
  );
});

test('READ_ONLY removes writes from any profile, deny beats allow', () => {
  const full = catalogFor({ PROFILE: 'full' });
  assert.ok(full.visible.some((tool) => !tool.classification.readOnly));

  const locked = catalogFor({ PROFILE: 'full', READ_ONLY: 'true' });
  assert.ok(
    locked.visible.every((tool) => tool.classification.readOnly),
    'READ_ONLY left a writing tool visible',
  );
  assert.ok(locked.visible.length > 100);

  const allowed = catalogFor({ PROFILE: 'readonly', TOOLS_ALLOW: 'change_work_order_status' });
  assert.ok(allowed.byName.has('change_work_order_status'));

  const denied = catalogFor({
    PROFILE: 'readonly',
    TOOLS_ALLOW: 'change_work_order_status',
    TOOLS_DENY: 'change_*',
  });
  assert.equal(denied.byName.has('change_work_order_status'), false);

  // READ_ONLY outranks an allow glob: the switch is meant to be the last word.
  const lockedAllow = catalogFor({
    PROFILE: 'readonly',
    READ_ONLY: 'true',
    TOOLS_ALLOW: 'change_work_order_status',
  });
  assert.equal(lockedAllow.byName.has('change_work_order_status'), false);
});

test('the assets profile keeps work orders out and stays useful', () => {
  const catalog = catalogFor({ PROFILE: 'assets' });
  assert.ok(catalog.byName.has('search_assets'));
  assert.ok(catalog.byName.has('get_meters_for_asset'));
  assert.equal(catalog.byName.has('search_work_orders'), false);
  assert.ok(catalog.visible.length > 10 && catalog.visible.length < 100);
});

test('core-readonly is small enough for a model to choose from', () => {
  const catalog = catalogFor({ PROFILE: 'core-readonly' });
  assert.ok(catalog.visible.length > 15, 'core-readonly is suspiciously empty');
  assert.ok(catalog.visible.length < 40, 'core-readonly has grown past a curated set');
  assert.ok(catalog.visible.every((tool) => tool.curated && tool.classification.readOnly));
});

test('the full profile covers the real surface and every tool is described', () => {
  const catalog = catalogFor({ PROFILE: 'full' });
  assert.ok(catalog.visible.length > 300, `only ${catalog.visible.length} tools visible`);
  for (const tool of catalog.visible) {
    assert.ok(tool.description.length > 20, `${tool.name} has no usable description`);
    assert.equal(tool.inputSchema.type, 'object');
    assert.equal(tool.inputSchema.additionalProperties, false);
  }
});

test('path parameters are required arguments, query parameters follow the document', () => {
  const catalog = catalogFor({ PROFILE: 'full' });
  const asset = catalog.byName.get('get_asset');
  assert.ok(asset);
  assert.deepEqual(asset.inputSchema.required, ['id']);

  const search = catalog.byName.get('search_assets');
  assert.ok(search);
  assert.deepEqual(search.inputSchema.required, ['body']);
  const properties = search.inputSchema.properties as Record<string, unknown>;
  assert.ok(properties.body, 'the request body should be nested under "body"');
});

test('an unknown profile fails at boot rather than serving nothing', () => {
  assert.throws(() => catalogFor({ PROFILE: 'nope' }), /Unknown PROFILE/);
});
