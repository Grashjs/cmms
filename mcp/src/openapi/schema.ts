import type { JsonSchema } from './types.js';

/**
 * Turns an OpenAPI schema into the JSON Schema an MCP tool advertises as its input.
 *
 * Two things make this small. First, this API's document is OpenAPI **3.1**, whose schemas
 * already *are* JSON Schema 2020-12 — there is no `nullable`, no `oneOf`/`anyOf`/`allOf`
 * anywhere in it, so no dialect translation is needed, only `$ref` resolution. Second, MCP
 * input schemas are consumed by models, not validators, so the goal is a readable shape
 * rather than a complete one.
 *
 * `$ref`s are inlined instead of emitted as `$defs`, because client-side support for
 * `$ref` in tool input schemas is uneven. Inlining needs two guards, and both of them bite
 * on this document:
 *
 * - **Cycles.** Entity DTOs reference each other (an asset has a parent asset). A repeated
 *   name on the current path becomes an untyped object.
 * - **Size.** `PreventiveMaintenancePostDTO` inlines to ~130 KB, which would cost more
 *   context than every other tool combined. Past `maxChars` the object is pruned to its
 *   top-level properties, and the model is told so in the description.
 */

export interface BuildOptions {
  schemas: Record<string, JsonSchema>;
  maxDepth: number;
  maxChars: number;
}

const REF_PREFIX = '#/components/schemas/';

/** Keys that carry no meaning for a model and only cost tokens. */
const DROPPED_KEYS = new Set(['xml', 'externalDocs', 'example', 'examples', 'discriminator']);

export function resolveSchema(schema: JsonSchema | undefined, options: BuildOptions): JsonSchema {
  if (!schema) return { type: 'object' };
  const inlined = inline(schema, options, [], 0);
  if (typeof inlined !== 'object' || inlined === null || Array.isArray(inlined)) {
    return { type: 'object' };
  }
  const result = inlined as JsonSchema;
  if (JSON.stringify(result).length <= options.maxChars) return result;
  return prune(result);
}

function inline(node: unknown, options: BuildOptions, stack: string[], depth: number): unknown {
  if (node === null || typeof node !== 'object') return node;
  if (Array.isArray(node)) return node.map((entry) => inline(entry, options, stack, depth));

  const source = node as JsonSchema;
  const ref = source.$ref;
  if (typeof ref === 'string') {
    if (!ref.startsWith(REF_PREFIX)) {
      // Nothing in this document uses external or non-schema refs; if that ever changes,
      // an untyped object is a safer answer than a broken reference.
      return { type: 'object', description: `unresolved reference ${ref}` };
    }
    const name = ref.slice(REF_PREFIX.length);
    if (stack.includes(name)) {
      return { type: 'object', description: `${name} (recursive reference, shape omitted)` };
    }
    if (depth >= options.maxDepth) {
      return { type: 'object', description: `${name} (nested deeper than MAX_SCHEMA_DEPTH)` };
    }
    const target = options.schemas[name];
    if (!target) return { type: 'object', description: `unknown schema ${name}` };
    const resolved = inline(target, options, [...stack, name], depth + 1);
    if (resolved && typeof resolved === 'object' && !Array.isArray(resolved)) {
      const withTitle = resolved as JsonSchema;
      // Keep the DTO name: it is often the only hint about what an object represents,
      // because the document carries a description for only 6 of its 373 operations.
      if (withTitle.title === undefined) withTitle.title = name;
    }
    return resolved;
  }

  const out: JsonSchema = {};
  for (const [key, value] of Object.entries(source)) {
    if (DROPPED_KEYS.has(key)) continue;
    out[key] = inline(value, options, stack, depth);
  }
  return out;
}

/**
 * Last resort for schemas that are too large to advertise in full: keep the top-level
 * properties but replace their contents with a type marker. The tool stays callable — the
 * CMMS validates the real payload anyway — and an agent that needs the detail can read the
 * full schema through the `cmms://schema/{name}` resource.
 */
function prune(schema: JsonSchema): JsonSchema {
  const properties = schema.properties;
  if (typeof properties !== 'object' || properties === null) return schema;

  const pruned: Record<string, JsonSchema> = {};
  for (const [name, raw] of Object.entries(properties as Record<string, JsonSchema>)) {
    const property = raw ?? {};
    const type = typeof property.type === 'string' ? property.type : 'object';
    const description = typeof property.description === 'string' ? property.description : undefined;
    if (type === 'object' || type === 'array') {
      pruned[name] = {
        type,
        description: [description, 'nested shape omitted (schema too large); read cmms://schema/' + (property.title ?? name) + ' for the full definition']
          .filter(Boolean)
          .join(' — '),
      };
    } else {
      pruned[name] = property;
    }
  }
  return { ...schema, properties: pruned };
}
