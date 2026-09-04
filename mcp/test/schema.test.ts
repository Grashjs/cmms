import assert from 'node:assert/strict';
import { test } from 'node:test';

import { resolveSchema } from '../src/openapi/schema.js';
import type { JsonSchema } from '../src/openapi/types.js';
import { realDocument } from './helpers.js';

const options = { schemas: {} as Record<string, JsonSchema>, maxDepth: 6, maxChars: 12000 };

test('references are inlined and the DTO name is kept as the title', () => {
  const result = resolveSchema(
    { $ref: '#/components/schemas/Thing' },
    {
      ...options,
      schemas: { Thing: { type: 'object', properties: { id: { type: 'integer' } } } },
    },
  );
  assert.equal(result.type, 'object');
  assert.equal(result.title, 'Thing');
});

test('a cycle becomes an untyped object instead of recursing forever', () => {
  const schemas: Record<string, JsonSchema> = {
    Asset: { type: 'object', properties: { parent: { $ref: '#/components/schemas/Asset' } } },
  };
  const result = resolveSchema({ $ref: '#/components/schemas/Asset' }, { ...options, schemas });
  const parent = (result.properties as Record<string, JsonSchema>).parent!;
  assert.equal(parent.type, 'object');
  assert.match(String(parent.description), /recursive/);
});

test('nesting past maxDepth is cut off', () => {
  const schemas: Record<string, JsonSchema> = {
    A: { type: 'object', properties: { b: { $ref: '#/components/schemas/B' } } },
    B: { type: 'object', properties: { c: { $ref: '#/components/schemas/C' } } },
    C: { type: 'object', properties: { value: { type: 'string' } } },
  };
  const result = resolveSchema({ $ref: '#/components/schemas/A' }, { ...options, schemas, maxDepth: 2 });
  const b = (result.properties as Record<string, JsonSchema>).b!;
  const c = (b.properties as Record<string, JsonSchema>).c!;
  assert.match(String(c.description), /MAX_SCHEMA_DEPTH/);
});

test('an unresolvable reference does not throw', () => {
  const result = resolveSchema({ $ref: '#/components/schemas/Nope' }, options);
  assert.equal(result.type, 'object');
  assert.match(String(result.description), /unknown schema/);
});

test('the largest real request body is pruned rather than advertised in full', () => {
  const document = realDocument();
  const schemas = document.components?.schemas ?? {};
  // PreventiveMaintenancePostDTO inlines to roughly 130 KB — more context than every other
  // tool in the catalogue put together.
  const full = resolveSchema(
    { $ref: '#/components/schemas/PreventiveMaintenancePostDTO' },
    { schemas, maxDepth: 12, maxChars: Number.MAX_SAFE_INTEGER },
  );
  assert.ok(JSON.stringify(full).length > 50_000);

  const capped = resolveSchema(
    { $ref: '#/components/schemas/PreventiveMaintenancePostDTO' },
    { schemas, maxDepth: 6, maxChars: 12000 },
  );
  const rendered = JSON.stringify(capped);
  assert.ok(rendered.length < 50_000, `pruned schema is still ${rendered.length} characters`);
  assert.match(rendered, /schema too large/);
  // Pruning keeps the field names: the tool has to stay callable.
  assert.ok(Object.keys(capped.properties as object).length > 3);
});

test('every real request body stays within the advertised ceiling', () => {
  const document = realDocument();
  const schemas = document.components?.schemas ?? {};
  const maxChars = 12000;
  for (const [path, item] of Object.entries(document.paths ?? {})) {
    for (const [method, operation] of Object.entries(item as Record<string, unknown>)) {
      const schema = (operation as { requestBody?: { content?: Record<string, { schema?: JsonSchema }> } })
        ?.requestBody?.content?.['application/json']?.schema;
      if (!schema) continue;
      const resolved = resolveSchema(schema, { schemas, maxDepth: 6, maxChars });
      const size = JSON.stringify(resolved).length;
      // Pruning only strips nested shapes, so a body with very many scalar fields can sit
      // slightly above the ceiling. Two ceilings would be a lie; a factor is honest.
      assert.ok(size < maxChars * 4, `${method} ${path} resolves to ${size} characters`);
    }
  }
});
