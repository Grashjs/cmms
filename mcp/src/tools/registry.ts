import type { Config } from '../config.js';
import type { Logger } from '../logger.js';
import { extractOperations, type Operation } from '../openapi/operations.js';
import { resolveSchema } from '../openapi/schema.js';
import type { JsonSchema, OpenApiDocument, OpenApiParameter } from '../openapi/types.js';
import { classify, isBlocked, synthesiseDescription, type Classification } from './describe.js';
import { curatedFor } from './curated.js';
import { matchesAnyGlob, toolNameFor } from './naming.js';
import { resolveProfile, type Profile } from './profiles.js';

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: JsonSchema;
  operation: Operation;
  classification: Classification;
  /** True when name and description come from the curated layer rather than being derived. */
  curated: boolean;
}

export interface ExcludedOperation {
  method: string;
  path: string;
  reason: string;
}

export interface Catalog {
  /** Every operation that could be a tool, whether the profile shows it or not. */
  all: ToolDefinition[];
  /** What this deployment offers, after profile, allow/deny globs and READ_ONLY. */
  visible: ToolDefinition[];
  byName: Map<string, ToolDefinition>;
  /** Expressible but filtered out. `list_capabilities` can still describe these. */
  hidden: ToolDefinition[];
  /** Not expressible as a JSON tool call, or never offered. */
  excluded: ExcludedOperation[];
  profile: Profile;
  document: { title: string; version: string; openapi: string };
}

export function buildCatalog(document: OpenApiDocument, config: Config, logger: Logger): Catalog {
  const profile = resolveProfile(config.profile);
  const schemas = document.components?.schemas ?? {};
  const schemaOptions = {
    schemas,
    maxDepth: config.maxSchemaDepth,
    maxChars: config.maxSchemaChars,
  };

  const all: ToolDefinition[] = [];
  const excluded: ExcludedOperation[] = [];
  const claimed = new Map<string, Operation>();

  for (const operation of extractOperations(document)) {
    if (operation.unsupported) {
      excluded.push({ method: operation.method, path: operation.path, reason: operation.unsupported });
      continue;
    }
    if (isBlocked(operation)) {
      excluded.push({
        method: operation.method,
        path: operation.path,
        reason: 'never offered as a tool by this server (credentials, session or billing surface)',
      });
      continue;
    }

    const curated = curatedFor(operation.method, operation.path);
    const name = curated?.name ?? toolNameFor(operation);

    const previous = claimed.get(name);
    if (previous) {
      // Generated names cannot collide (method + path is unique), so this can only be a
      // mistake in the curated table. Keep the first and say which one lost, loudly.
      logger.error('tool name collision, second operation dropped', {
        name,
        kept: `${previous.method.toUpperCase()} ${previous.path}`,
        dropped: `${operation.method.toUpperCase()} ${operation.path}`,
      });
      excluded.push({
        method: operation.method,
        path: operation.path,
        reason: `tool name ${name} already taken by ${previous.method.toUpperCase()} ${previous.path}`,
      });
      continue;
    }
    claimed.set(name, operation);

    all.push({
      name,
      description: curated?.description ?? synthesiseDescription(operation),
      inputSchema: buildInputSchema(operation, schemaOptions),
      operation,
      classification: classify(operation),
      curated: curated !== undefined,
    });
  }

  const visible: ToolDefinition[] = [];
  const hidden: ToolDefinition[] = [];
  for (const tool of all) {
    (isVisible(tool, config, profile) ? visible : hidden).push(tool);
  }

  return {
    all,
    visible,
    hidden,
    byName: new Map(visible.map((tool) => [tool.name, tool])),
    excluded,
    profile,
    document: {
      title: document.info?.title ?? 'CMMS',
      version: document.info?.version ?? 'unknown',
      openapi: document.openapi ?? 'unknown',
    },
  };
}

/**
 * Visibility, in order: a deny glob always wins; READ_ONLY removes every writing tool; an
 * allow glob adds a tool the profile does not include; otherwise the profile decides.
 */
function isVisible(tool: ToolDefinition, config: Config, profile: Profile): boolean {
  if (matchesAnyGlob(tool.name, config.toolsDeny)) return false;
  if (config.readOnly && !tool.classification.readOnly) return false;
  if (matchesAnyGlob(tool.name, config.toolsAllow)) return true;
  return profile.includes(tool);
}

interface SchemaOptions {
  schemas: Record<string, JsonSchema>;
  maxDepth: number;
  maxChars: number;
}

/**
 * A tool's arguments are the path parameters and query parameters as top-level properties,
 * plus the request body under a single `body` property.
 *
 * The body is nested rather than merged so a body field can never collide with, or be
 * mistaken for, a query parameter — the two go to different places in the HTTP request and
 * flattening them would make a mis-sent field look like a server bug.
 */
function buildInputSchema(operation: Operation, options: SchemaOptions): JsonSchema {
  const properties: Record<string, JsonSchema> = {};
  const required: string[] = [];

  for (const parameter of operation.pathParams) {
    properties[parameter.name] = parameterSchema(parameter, options, 'path parameter');
    required.push(parameter.name);
  }

  for (const parameter of operation.queryParams) {
    if (properties[parameter.name]) continue;
    properties[parameter.name] = parameterSchema(parameter, options, 'query parameter');
    if (parameter.required) required.push(parameter.name);
  }

  if (operation.body) {
    const schema = resolveSchema(operation.body.schema, options);
    properties.body = {
      ...schema,
      description: [schema.description, 'Request body.'].filter(Boolean).join(' '),
    };
    if (operation.body.required) required.push('body');
  }

  return {
    type: 'object',
    properties,
    ...(required.length > 0 ? { required } : {}),
    additionalProperties: false,
  };
}

function parameterSchema(
  parameter: OpenApiParameter,
  options: SchemaOptions,
  kind: string,
): JsonSchema {
  const schema = resolveSchema(parameter.schema, options);
  const description = [parameter.description, `(${kind})`].filter(Boolean).join(' ');
  return { ...schema, description };
}
