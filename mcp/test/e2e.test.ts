import assert from 'node:assert/strict';
import { createServer, type Server as HttpServer } from 'node:http';
import { after, before, test } from 'node:test';

import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';

import { loadConfig } from '../src/config.js';
import { createLogger } from '../src/logger.js';
import { RateLimiter } from '../src/ratelimit.js';
import type { ServerContext } from '../src/server.js';
import { buildCatalog } from '../src/tools/registry.js';
import { startHttpTransport, type HttpServerHandle } from '../src/transport/http.js';
import { realDocument } from './helpers.js';

/**
 * The whole chain, in process: an MCP client speaks Streamable HTTP to this server, which
 * proxies to a stand-in for the CMMS. It proves the parts that unit tests cannot — that the
 * `x-api-key` header survives the trip from MCP request to REST call, that a 403 comes back
 * as a structured failure rather than a protocol error, and that a hidden tool stays
 * uncallable.
 */

interface RecordedCall {
  method: string;
  url: string;
  apiKey: string | undefined;
  body: string;
}

let cmms: HttpServer;
let cmmsPort = 0;
let mcp: HttpServerHandle;
const calls: RecordedCall[] = [];
const auditLines: string[] = [];

before(async () => {
  cmms = createServer((request, response) => {
    let body = '';
    request.on('data', (chunk) => (body += chunk));
    request.on('end', () => {
      calls.push({
        method: request.method ?? '',
        url: request.url ?? '',
        apiKey: request.headers['x-api-key'] as string | undefined,
        body,
      });

      const send = (status: number, payload: unknown) => {
        response.writeHead(status, { 'content-type': 'application/json' });
        response.end(JSON.stringify(payload));
      };

      if (request.url === '/assets/1') return send(200, { id: 1, name: 'Air handling unit 3' });
      if (request.url === '/assets/999') return send(404, { success: false, message: 'Not found' });
      if (request.url === '/assets/search') return send(200, { content: [{ id: 1 }], totalElements: 1 });
      if (request.url === '/locations/mini') {
        return send(403, { success: false, message: 'Access denied' });
      }
      return send(500, { success: false, message: 'unexpected path ' + request.url });
    });
  });
  await new Promise<void>((resolve) => cmms.listen(0, '127.0.0.1', resolve));
  cmmsPort = (cmms.address() as { port: number }).port;

  const config = loadConfig({
    CMMS_BASE_URL: `http://127.0.0.1:${cmmsPort}`,
    PROFILE: 'core',
    PORT: '0',
    HOST: '127.0.0.1',
  } as NodeJS.ProcessEnv);
  const logger = createLogger(config, { write: (line) => auditLines.push(line) });
  const document = realDocument();
  const context: ServerContext = {
    config,
    logger,
    document,
    catalog: buildCatalog(document, config, logger),
    rateLimiter: new RateLimiter({ perMinute: 0, burst: 1 }),
  };
  mcp = await startHttpTransport(context);
});

after(async () => {
  await mcp.close();
  await new Promise<void>((resolve) => cmms.close(() => resolve()));
});

async function connect(headers: Record<string, string> = { 'x-api-key': 'test-key' }) {
  const client = new Client({ name: 'e2e', version: '0' });
  const transport = new StreamableHTTPClientTransport(
    new URL(`http://127.0.0.1:${mcp.port}/mcp`),
    { requestInit: { headers } },
  );
  await client.connect(transport);
  return client;
}

test('a client can list the curated tools and the discovery tool', async () => {
  const client = await connect();
  try {
    const { tools } = await client.listTools();
    const names = tools.map((tool) => tool.name);
    assert.ok(names.includes('get_asset'));
    assert.ok(names.includes('search_assets'));
    assert.ok(names.includes('list_capabilities'));

    const asset = tools.find((tool) => tool.name === 'get_asset');
    assert.equal(asset?.annotations?.readOnlyHint, true);
    assert.equal(asset?.inputSchema.type, 'object');

    const status = tools.find((tool) => tool.name === 'change_work_order_status');
    assert.equal(status?.annotations?.readOnlyHint, false);
    assert.equal(status?.annotations?.destructiveHint, true);
  } finally {
    await client.close();
  }
});

test('a tool call reaches the CMMS carrying the client\'s own key', async () => {
  const client = await connect({ 'x-api-key': 'caller-key' });
  try {
    const result = await client.callTool({ name: 'get_asset', arguments: { id: 1 } });
    assert.equal(result.isError, undefined);
    const text = (result.content as { type: string; text: string }[])[0]!.text;
    assert.match(text, /Air handling unit 3/);

    const call = calls.at(-1);
    assert.equal(call?.url, '/assets/1');
    // The point of passthrough: the key is the caller's, not the server's.
    assert.equal(call?.apiKey, 'caller-key');
  } finally {
    await client.close();
  }
});

test('a search sends the body the client supplied', async () => {
  const client = await connect();
  try {
    await client.callTool({
      name: 'search_assets',
      arguments: { body: { pageNum: 0, pageSize: 5, filterFields: [] } },
    });
    const call = calls.at(-1);
    assert.equal(call?.method, 'POST');
    assert.equal(call?.url, '/assets/search');
    assert.deepEqual(JSON.parse(call!.body), { pageNum: 0, pageSize: 5, filterFields: [] });
  } finally {
    await client.close();
  }
});

test('a refusal arrives as a structured result, not a protocol error', async () => {
  const client = await connect();
  try {
    const notFound = await client.callTool({ name: 'get_asset', arguments: { id: 999 } });
    assert.equal(notFound.isError, true);
    const failure = JSON.parse((notFound.content as { text: string }[])[0]!.text) as {
      kind: string;
      retryable: boolean;
      advice: string;
    };
    assert.equal(failure.kind, 'not_found');
    assert.equal(failure.retryable, false);
    assert.ok(failure.advice.length > 0);

    const denied = await client.callTool({ name: 'list_locations_mini', arguments: {} });
    const deniedFailure = JSON.parse((denied.content as { text: string }[])[0]!.text) as {
      kind: string;
      message: string;
    };
    assert.equal(deniedFailure.kind, 'forbidden');
    assert.match(deniedFailure.message, /SELF_HOSTED_UNLOCK_PREMIUM/);
  } finally {
    await client.close();
  }
});

test('without a key no request reaches the CMMS at all', async () => {
  const client = await connect({});
  try {
    const before = calls.length;
    const result = await client.callTool({ name: 'get_asset', arguments: { id: 1 } });
    assert.equal(result.isError, true);
    const failure = JSON.parse((result.content as { text: string }[])[0]!.text) as { kind: string };
    assert.equal(failure.kind, 'unauthenticated');
    assert.equal(calls.length, before, 'the CMMS was called without a key');
  } finally {
    await client.close();
  }
});

test('a tool the profile hides cannot be called, and says so', async () => {
  const client = await connect();
  try {
    // Present in the document, absent from the `core` profile.
    const result = await client.callTool({ name: 'delete_assets_by_id', arguments: { id: 1 } });
    assert.equal(result.isError, true);
    const failure = JSON.parse((result.content as { text: string }[])[0]!.text) as {
      kind: string;
      message: string;
    };
    assert.equal(failure.kind, 'forbidden');
    assert.match(failure.message, /not enabled in this deployment/);
  } finally {
    await client.close();
  }
});

test('discovery finds a capability the profile hides', async () => {
  const client = await connect();
  try {
    const result = await client.callTool({
      name: 'list_capabilities',
      arguments: { query: 'downtime', include: 'all' },
    });
    const report = JSON.parse((result.content as { text: string }[])[0]!.text) as {
      matched: number;
      results: { name: string; enabled: boolean }[];
    };
    assert.ok(report.matched > 0);
    assert.ok(report.results.some((entry) => !entry.enabled));
  } finally {
    await client.close();
  }
});

test('resources and prompts are served', async () => {
  const client = await connect();
  try {
    const { resources } = await client.listResources();
    assert.ok(resources.some((resource) => resource.uri === 'cmms://capabilities'));

    const read = await client.readResource({ uri: 'cmms://enums/priority' });
    assert.match((read.contents[0] as { text: string }).text, /HIGH/);

    const { prompts } = await client.listPrompts();
    assert.ok(prompts.some((prompt) => prompt.name === 'asset_maintenance_summary'));

    const prompt = await client.getPrompt({
      name: 'asset_maintenance_summary',
      arguments: { asset: 'AHU 3' },
    });
    assert.match((prompt.messages[0]!.content as { text: string }).text, /AHU 3/);
  } finally {
    await client.close();
  }
});

test('the audit log records the call without the key or the payload', async () => {
  const client = await connect({ 'x-api-key': 'secret-key-value' });
  try {
    auditLines.length = 0;
    await client.callTool({ name: 'get_asset', arguments: { id: 1 } });
    const audit = auditLines
      .map((line) => JSON.parse(line) as Record<string, unknown>)
      .find((entry) => entry.event === 'tool_call');
    assert.ok(audit, 'no tool_call audit line');
    assert.equal(audit.tool, 'get_asset');
    assert.equal(audit.path, '/assets/{id}');
    assert.equal(audit.status, 200);
    assert.ok(typeof audit.key === 'string' && (audit.key as string).length === 12);
    const joined = auditLines.join('\n');
    assert.equal(joined.includes('secret-key-value'), false, 'the API key was logged');
    assert.equal(joined.includes('Air handling unit'), false, 'response data was logged');
  } finally {
    await client.close();
  }
});

test('health answers without a key and GET on the MCP endpoint is refused', async () => {
  const health = await fetch(`http://127.0.0.1:${mcp.port}/healthz`);
  assert.equal(health.status, 200);
  const payload = (await health.json()) as { status: string; tools: number };
  assert.equal(payload.status, 'ok');
  assert.ok(payload.tools > 0);

  const stream = await fetch(`http://127.0.0.1:${mcp.port}/mcp`);
  assert.equal(stream.status, 405);
});

test('the rate limit refuses a runaway caller', async () => {
  const config = loadConfig({
    CMMS_BASE_URL: `http://127.0.0.1:${cmmsPort}`,
    PROFILE: 'core',
    PORT: '0',
    HOST: '127.0.0.1',
    RATE_LIMIT: '60',
    RATE_LIMIT_BURST: '2',
  } as NodeJS.ProcessEnv);
  const logger = createLogger(config, { write: () => undefined });
  const document = realDocument();
  const limited = await startHttpTransport({
    config,
    logger,
    document,
    catalog: buildCatalog(document, config, logger),
    rateLimiter: new RateLimiter({ perMinute: 60, burst: 2 }),
  });
  try {
    const client = new Client({ name: 'e2e-limit', version: '0' });
    await client.connect(
      new StreamableHTTPClientTransport(new URL(`http://127.0.0.1:${limited.port}/mcp`), {
        requestInit: { headers: { 'x-api-key': 'loop-key' } },
      }),
    );
    const kinds: (string | undefined)[] = [];
    for (let index = 0; index < 4; index += 1) {
      const result = await client.callTool({ name: 'get_asset', arguments: { id: 1 } });
      kinds.push(
        result.isError
          ? (JSON.parse((result.content as { text: string }[])[0]!.text) as { kind: string }).kind
          : undefined,
      );
    }
    await client.close();
    assert.equal(kinds[0], undefined);
    assert.equal(kinds[1], undefined);
    assert.equal(kinds.at(-1), 'rate_limited');
  } finally {
    await limited.close();
  }
});
