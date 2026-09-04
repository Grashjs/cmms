import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import {
  CallToolRequestSchema,
  GetPromptRequestSchema,
  ListPromptsRequestSchema,
  ListResourceTemplatesRequestSchema,
  ListResourcesRequestSchema,
  ListToolsRequestSchema,
  ReadResourceRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';

import { resolveCaller, type IncomingHeaders } from './auth.js';
import { callOperation } from './cmms/client.js';
import { failure, type Failure } from './cmms/errors.js';
import type { Config } from './config.js';
import type { Logger } from './logger.js';
import type { OpenApiDocument } from './openapi/types.js';
import { findPrompt, PROMPTS } from './prompts.js';
import type { RateLimiter } from './ratelimit.js';
import { readResource, resourceTemplates, staticResources } from './resources.js';
import { matchesGlob } from './tools/naming.js';
import type { Catalog, ToolDefinition } from './tools/registry.js';

/**
 * The MCP surface. Built on the SDK's low-level `Server` rather than `McpServer`, and
 * deliberately so: `McpServer.registerTool` takes Zod schemas, but this server's input
 * schemas are derived from the OpenAPI document *at runtime*. Handing them to `McpServer`
 * would mean converting JSON Schema to Zod and back on every boot — a lossy round trip
 * and a dependency — to describe schemas the SDK would then serialise straight back to
 * JSON Schema. The low-level handler passes them through as they are.
 */

export interface ServerContext {
  config: Config;
  logger: Logger;
  catalog: Catalog;
  document: OpenApiDocument;
  rateLimiter: RateLimiter;
}

const META_TOOL = 'list_capabilities';

const SERVER_INFO = { name: 'cmms4fm-mcp', version: '0.1.0' };

export function createMcpServer(context: ServerContext): Server {
  const { config, logger } = context;
  // Handlers read `context.catalog` rather than a captured copy: SPEC_REFRESH_MINUTES can
  // replace the catalogue under a long-lived stdio server, and a captured reference would
  // keep serving the tool set from boot.

  const server = new Server(SERVER_INFO, {
    capabilities: {
      tools: {},
      ...(config.enableResources ? { resources: {} } : {}),
      ...(config.enablePrompts ? { prompts: {} } : {}),
    },
    instructions: instructions(context),
  });

  server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [metaToolDefinition(context.catalog), ...context.catalog.visible.map(toMcpTool)],
  }));

  server.setRequestHandler(CallToolRequestSchema, async (request, extra) => {
    const name = request.params.name;
    const args = (request.params.arguments ?? {}) as Record<string, unknown>;
    const headers = extra.requestInfo?.headers as IncomingHeaders;

    if (name === META_TOOL) {
      return textResult(JSON.stringify(describeCapabilities(context.catalog, args), null, 2));
    }

    const tool = context.catalog.byName.get(name);
    if (!tool) {
      const hidden = context.catalog.hidden.find((candidate) => candidate.name === name);
      logger.audit({ event: 'tool_unknown', tool: name, sessionId: extra.sessionId });
      return errorResult(
        hidden
          ? failure(
              'forbidden',
              `the tool "${name}" exists but is not enabled in this deployment (profile "${context.catalog.profile.name}"). Call ${META_TOOL} to see what is available.`,
            )
          : failure('invalid_input', `unknown tool "${name}"`),
      );
    }

    const resolution = resolveCaller(config, headers);
    if (!resolution.ok) {
      logger.audit({
        event: 'tool_denied',
        tool: tool.name,
        method: tool.operation.method.toUpperCase(),
        path: tool.operation.path,
        kind: resolution.failure.kind,
        sessionId: extra.sessionId,
      });
      return errorResult(resolution.failure);
    }
    const caller = resolution.caller;

    const decision = context.rateLimiter.take(caller.fingerprint);
    if (!decision.allowed) {
      logger.audit({
        event: 'tool_rate_limited',
        tool: tool.name,
        key: caller.fingerprint,
        sessionId: extra.sessionId,
      });
      return errorResult(
        failure(
          'rate_limited',
          `this API key exceeded ${config.rateLimitPerMinute} calls per minute on the MCP server; wait ${decision.retryAfterSeconds} s`,
        ),
      );
    }

    const result = await callOperation(config, tool.operation, args, {
      apiKey: caller.apiKey,
      signal: extra.signal,
    });

    logger.audit({
      event: 'tool_call',
      tool: tool.name,
      method: tool.operation.method.toUpperCase(),
      path: tool.operation.path,
      status: result.status,
      kind: result.failure?.kind,
      durationMs: result.durationMs,
      key: caller.fingerprint,
      keySource: caller.source,
      sessionId: extra.sessionId,
      readOnly: tool.classification.readOnly,
      truncated: result.truncated,
    });

    if (!result.ok && result.failure) return errorResult(result.failure);

    const rendered =
      typeof result.body === 'string' ? result.body : JSON.stringify(result.body, null, 2);
    const note = result.truncated
      ? `\n\n[truncated at ${config.maxResponseChars} characters — narrow the query or use pageSize to get less at a time]`
      : '';
    return textResult(rendered.length > 0 ? rendered + note : '(empty response)');
  });

  if (config.enableResources) {
    server.setRequestHandler(ListResourcesRequestSchema, async () => ({
      resources: staticResources(),
    }));
    server.setRequestHandler(ListResourceTemplatesRequestSchema, async () => ({
      resourceTemplates: resourceTemplates(),
    }));
    server.setRequestHandler(ReadResourceRequestSchema, async (request) => {
      const uri = request.params.uri;
      try {
        return { contents: [readResource(uri, context.catalog, context.document)] };
      } catch (error) {
        // Resources have no isError channel, so an unreadable URI has to throw.
        throw error instanceof Error ? error : new Error(String(error));
      }
    });
  }

  if (config.enablePrompts) {
    server.setRequestHandler(ListPromptsRequestSchema, async () => ({
      prompts: PROMPTS.map((prompt) => ({
        name: prompt.name,
        title: prompt.title,
        description: prompt.description,
        arguments: prompt.arguments,
      })),
    }));
    server.setRequestHandler(GetPromptRequestSchema, async (request) => {
      const prompt = findPrompt(request.params.name);
      if (!prompt) throw new Error(`unknown prompt "${request.params.name}"`);
      const args = (request.params.arguments ?? {}) as Record<string, string>;
      for (const argument of prompt.arguments) {
        if (argument.required && !args[argument.name]) {
          throw new Error(`prompt "${prompt.name}" requires the argument "${argument.name}"`);
        }
      }
      return {
        description: prompt.description,
        messages: [
          { role: 'user' as const, content: { type: 'text' as const, text: prompt.render(args) } },
        ],
      };
    });
  }

  return server;
}

function toMcpTool(tool: ToolDefinition) {
  return {
    name: tool.name,
    description: tool.description,
    inputSchema: tool.inputSchema,
    annotations: {
      title: `${tool.operation.method.toUpperCase()} ${tool.operation.path}`,
      readOnlyHint: tool.classification.readOnly,
      destructiveHint: tool.classification.destructive,
      idempotentHint: tool.classification.idempotent,
      // Every tool reaches a live CMMS whose data other people change.
      openWorldHint: true,
    },
  };
}

/**
 * Discovery instead of a tool flood (konzept §4.3). 373 operations cannot all be tools
 * without wrecking a model's ability to choose, so the profile shows a subset and this tool
 * describes the rest.
 *
 * It is discovery only: naming a hidden tool here does not make it callable. That is the
 * point — an agent learns that a capability exists and can say so, instead of concluding
 * the CMMS cannot do it, and a person then widens the profile.
 */
function metaToolDefinition(catalog: Catalog) {
  return {
    name: META_TOOL,
    description: [
      `Search the whole CMMS API surface — ${catalog.all.length} operations — including the ones this deployment does not currently offer as tools.`,
      `Right now ${catalog.visible.length} are enabled by the profile "${catalog.profile.name}" and ${catalog.hidden.length} are hidden.`,
      'Use it when a task seems impossible with the tools you can see: if the capability exists but is hidden, say so and name it rather than concluding the CMMS cannot do it. Calling a hidden tool still fails.',
    ].join(' '),
    inputSchema: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description:
            'Free text matched against tool name, path and tag, e.g. "downtime", "purchase order", "meter".',
        },
        tag: { type: 'string', description: 'Restrict to one API tag, e.g. "Assets".' },
        method: {
          type: 'string',
          enum: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
          description: 'Restrict to one HTTP method.',
        },
        include: {
          type: 'string',
          enum: ['visible', 'hidden', 'all'],
          description: 'Which part of the surface to search. Defaults to "all".',
        },
        limit: { type: 'integer', description: 'Maximum results (default 40).' },
      },
      additionalProperties: false,
    },
    annotations: {
      title: 'Discover CMMS capabilities',
      readOnlyHint: true,
      destructiveHint: false,
      idempotentHint: true,
      openWorldHint: false,
    },
  };
}

function describeCapabilities(catalog: Catalog, args: Record<string, unknown>): unknown {
  const include = typeof args.include === 'string' ? args.include : 'all';
  const limit = typeof args.limit === 'number' && args.limit > 0 ? Math.min(args.limit, 400) : 40;
  const query = typeof args.query === 'string' ? args.query.toLowerCase().trim() : '';
  const tag = typeof args.tag === 'string' ? args.tag.toLowerCase() : '';
  const method = typeof args.method === 'string' ? args.method.toLowerCase() : '';

  const pool =
    include === 'visible' ? catalog.visible : include === 'hidden' ? catalog.hidden : catalog.all;

  const matches = pool.filter((tool) => {
    if (tag && tool.operation.tag.toLowerCase() !== tag) return false;
    if (method && tool.operation.method !== method) return false;
    if (!query) return true;
    const haystack = `${tool.name} ${tool.operation.path} ${tool.operation.tag} ${tool.description}`.toLowerCase();
    return query.split(/\s+/).every((term) => haystack.includes(term) || matchesGlob(tool.name, term));
  });

  return {
    profile: { name: catalog.profile.name, description: catalog.profile.description },
    enabledTools: catalog.visible.length,
    hiddenTools: catalog.hidden.length,
    matched: matches.length,
    returned: Math.min(matches.length, limit),
    results: matches.slice(0, limit).map((tool) => ({
      name: tool.name,
      endpoint: `${tool.operation.method.toUpperCase()} ${tool.operation.path}`,
      tag: tool.operation.tag,
      readOnly: tool.classification.readOnly,
      enabled: catalog.byName.has(tool.name),
      curated: tool.curated,
      description: tool.description.slice(0, 240),
      requiredArguments: (tool.inputSchema.required as string[] | undefined) ?? [],
    })),
    ...(matches.length > limit
      ? { note: `${matches.length - limit} further matches not shown; narrow the query or raise limit` }
      : {}),
  };
}

function instructions(context: ServerContext): string {
  const { catalog, config } = context;
  return [
    `Tools over the ${catalog.document.title} of this organisation, proxied to its REST API.`,
    `Profile "${catalog.profile.name}": ${catalog.profile.description}`,
    config.readOnly ? 'READ_ONLY is on, so no writing tool is offered at all.' : '',
    `${catalog.visible.length} tools are enabled; ${META_TOOL} searches the remaining ${catalog.hidden.length}.`,
    'Every call runs as the user the API key belongs to and sees only that user\'s organisation. A 403 means that user lacks the permission, not that the record is missing.',
    'List endpoints are POST with a filter body, not GET: search_assets, search_work_orders, and so on. Read cmms://enums before filtering on a status or priority — those columns hold enum ordinals, so a wrong value matches nothing and reports no error.',
  ]
    .filter((line) => line !== '')
    .join('\n');
}

function textResult(text: string) {
  return { content: [{ type: 'text' as const, text }] };
}

/**
 * Failures travel in-band with `isError: true` rather than as JSON-RPC errors: a refused
 * call is a result the agent has to reason about, not a protocol fault. The payload is
 * structured so it can act on `kind` and `retryable` instead of parsing prose.
 */
function errorResult(reason: Failure) {
  return {
    isError: true,
    content: [{ type: 'text' as const, text: JSON.stringify(reason, null, 2) }],
  };
}
