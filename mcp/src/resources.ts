import type { Catalog } from './tools/registry.js';
import type { JsonSchema, OpenApiDocument } from './openapi/types.js';

/**
 * Resources (konzept §7): the things an agent should *read* before it acts, without
 * spending a tool call and a round trip on each of them.
 *
 * All three are derived from the OpenAPI document the server already holds, so reading one
 * needs no API key and touches the CMMS not at all:
 *
 * - `cmms://capabilities` — the whole tool surface, including what this profile hides. The
 *   answer to "what could this server do" without 300 tool definitions in the context.
 * - `cmms://enums` and `cmms://enums/{name}` — the allowed values for status, priority and
 *   the rest. Enum columns are stored as ordinals in this CMMS, so a filter carrying an
 *   invented value matches nothing and reports no error; the values have to be exact.
 * - `cmms://schema/{name}` — the full DTO definition, including the parts a tool's input
 *   schema had to prune to stay small.
 */

export interface ResourceDescriptor {
  uri: string;
  name: string;
  title?: string;
  description: string;
  mimeType: string;
}

export interface ResourceTemplateDescriptor {
  uriTemplate: string;
  name: string;
  description: string;
  mimeType: string;
}

export interface ResourceContents {
  uri: string;
  mimeType: string;
  text: string;
}

export interface EnumEntry {
  name: string;
  values: string[];
  /** Schemas the enum was found in, so an agent knows where it applies. */
  usedIn: string[];
}

export function staticResources(): ResourceDescriptor[] {
  return [
    {
      uri: 'cmms://capabilities',
      name: 'capabilities',
      title: 'Tool surface of this CMMS',
      description:
        'Every operation this server could offer, which of them the active profile shows, and why the rest are hidden. Read this to find out whether a capability exists before assuming it does not.',
      mimeType: 'application/json',
    },
    {
      uri: 'cmms://enums',
      name: 'enums',
      title: 'Enumerations',
      description:
        'All enumerated value sets in the API (status, priority, recurrence, …). Filters on these fields must use the exact value; an unknown one matches nothing without failing.',
      mimeType: 'application/json',
    },
  ];
}

export function resourceTemplates(): ResourceTemplateDescriptor[] {
  return [
    {
      uriTemplate: 'cmms://enums/{name}',
      name: 'enum',
      description: 'The allowed values of one enumeration, e.g. cmms://enums/priority.',
      mimeType: 'application/json',
    },
    {
      uriTemplate: 'cmms://schema/{name}',
      name: 'schema',
      description:
        'The full definition of one DTO, e.g. cmms://schema/AssetShowDTO. Use it when a tool input schema says a nested shape was omitted.',
      mimeType: 'application/json',
    },
  ];
}

export function readResource(
  uri: string,
  catalog: Catalog,
  document: OpenApiDocument,
): ResourceContents {
  if (uri === 'cmms://capabilities') {
    return json(uri, capabilityReport(catalog));
  }
  if (uri === 'cmms://enums') {
    return json(uri, { enums: collectEnums(document) });
  }

  const enumMatch = /^cmms:\/\/enums\/(.+)$/.exec(uri);
  if (enumMatch) {
    const wanted = decodeURIComponent(enumMatch[1]!).toLowerCase();
    const found = collectEnums(document).find((entry) => entry.name.toLowerCase() === wanted);
    if (!found) {
      throw new Error(`unknown enumeration "${wanted}"; read cmms://enums for the list`);
    }
    return json(uri, found);
  }

  const schemaMatch = /^cmms:\/\/schema\/(.+)$/.exec(uri);
  if (schemaMatch) {
    const name = decodeURIComponent(schemaMatch[1]!);
    const schemas = document.components?.schemas ?? {};
    const schema = schemas[name];
    if (!schema) {
      const near = Object.keys(schemas)
        .filter((candidate) => candidate.toLowerCase().includes(name.toLowerCase()))
        .slice(0, 10);
      throw new Error(
        `unknown schema "${name}"${near.length > 0 ? `; did you mean ${near.join(', ')}` : ''}`,
      );
    }
    // Handed over unresolved: `$ref`s stay as written so the agent can follow them one at a
    // time instead of receiving a 130 KB inlined graph.
    return json(uri, { name, schema });
  }

  throw new Error(`unknown resource ${uri}`);
}

export function capabilityReport(catalog: Catalog): unknown {
  return {
    api: catalog.document,
    profile: { name: catalog.profile.name, description: catalog.profile.description },
    counts: {
      visible: catalog.visible.length,
      hidden: catalog.hidden.length,
      excluded: catalog.excluded.length,
    },
    visible: catalog.visible.map(summarise),
    hidden: catalog.hidden.map(summarise),
    excluded: catalog.excluded,
  };
}

function summarise(tool: Catalog['all'][number]) {
  return {
    name: tool.name,
    method: tool.operation.method.toUpperCase(),
    path: tool.operation.path,
    tag: tool.operation.tag,
    readOnly: tool.classification.readOnly,
    curated: tool.curated,
  };
}

/**
 * Collects enumerations by the property name they appear under, merging identical sets. The
 * document has no standalone enum schemas — springdoc inlines them into each property — so
 * the property name is the only handle available.
 */
export function collectEnums(document: OpenApiDocument): EnumEntry[] {
  const found = new Map<string, { values: Set<string>; usedIn: Set<string> }>();

  const visit = (node: unknown, propertyName: string, schemaName: string): void => {
    if (node === null || typeof node !== 'object') return;
    if (Array.isArray(node)) {
      for (const entry of node) visit(entry, propertyName, schemaName);
      return;
    }
    const schema = node as JsonSchema;
    const values = schema.enum;
    if (Array.isArray(values) && values.every((value) => typeof value === 'string')) {
      const entry = found.get(propertyName) ?? { values: new Set(), usedIn: new Set() };
      for (const value of values as string[]) entry.values.add(value);
      entry.usedIn.add(schemaName);
      found.set(propertyName, entry);
    }
    for (const [key, value] of Object.entries(schema)) {
      if (key === 'properties' && value && typeof value === 'object') {
        for (const [property, child] of Object.entries(value as Record<string, unknown>)) {
          visit(child, property, schemaName);
        }
        continue;
      }
      visit(value, propertyName, schemaName);
    }
  };

  for (const [name, schema] of Object.entries(document.components?.schemas ?? {})) {
    visit(schema, name, name);
  }

  return [...found.entries()]
    .map(([name, entry]) => ({
      name,
      values: [...entry.values].sort(),
      usedIn: [...entry.usedIn].sort().slice(0, 12),
    }))
    .sort((a, b) => a.name.localeCompare(b.name));
}

function json(uri: string, payload: unknown): ResourceContents {
  return { uri, mimeType: 'application/json', text: JSON.stringify(payload, null, 2) };
}
