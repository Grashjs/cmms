import { createServer, type IncomingMessage, type ServerResponse } from 'node:http';

import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';

import { createMcpServer, type ServerContext } from '../server.js';

/**
 * Streamable HTTP, stateless (konzept E7 and §4.1).
 *
 * Stateless is not a simplification here, it is the architecture: the server holds no
 * session state, so any replica can answer any request and a restart loses nothing. Each
 * POST gets its own `Server` and transport, which costs almost nothing because the
 * expensive part — parsing the OpenAPI document and building the tool catalogue — happened
 * once at boot and is shared.
 *
 * Consequence: there is no server-initiated channel, so `GET /mcp` (the SSE stream) and
 * `DELETE /mcp` (session teardown) answer 405 rather than pretending to work.
 */

export interface HttpServerHandle {
  close(): Promise<void>;
  port: number;
}

export function startHttpTransport(context: ServerContext): Promise<HttpServerHandle> {
  const { config, logger } = context;

  const httpServer = createServer((request, response) => {
    void handle(request, response, context).catch((error: unknown) => {
      logger.error('unhandled failure while serving a request', {
        error: error instanceof Error ? error.message : String(error),
        url: request.url,
      });
      if (!response.headersSent) {
        respondJson(response, 500, { error: 'internal error' });
      } else {
        response.end();
      }
    });
  });

  return new Promise((resolve, reject) => {
    httpServer.once('error', reject);
    httpServer.listen(config.port, config.host, () => {
      httpServer.removeListener('error', reject);
      const address = httpServer.address();
      // PORT=0 asks the operating system for a free port, which the tests use; report the
      // one actually bound rather than the one requested.
      const port = typeof address === 'object' && address !== null ? address.port : config.port;
      logger.info('MCP server listening', {
        host: config.host,
        port,
        endpoint: '/mcp',
        profile: context.catalog.profile.name,
        tools: context.catalog.visible.length,
      });
      resolve({
        port,
        close: () =>
          new Promise<void>((done, fail) => {
            httpServer.close((error) => (error ? fail(error) : done()));
          }),
      });
    });
  });
}

async function handle(
  request: IncomingMessage,
  response: ServerResponse,
  context: ServerContext,
): Promise<void> {
  const url = new URL(request.url ?? '/', 'http://localhost');
  // nginx may forward `/mcp/...` untouched or strip the prefix, depending on whether its
  // proxy_pass carries a trailing slash. Accepting both spellings means the route works
  // either way instead of turning a one-character config detail into a 404.
  const path = (url.pathname.replace(/\/+$/, '') || '/').replace(/^\/mcp(?=$|\/)/, '') || '/';

  // Readiness is "the OpenAPI document is loaded" (konzept §6) — and by the time this
  // server is listening at all, it is. Reaching this handler is therefore the answer.
  // Probe this, never the MCP endpoint: a probe that POSTs JSON-RPC would open sessions.
  if (request.method === 'GET' && (path === '/healthz' || path === '/readyz')) {
    respondJson(response, 200, {
      status: 'ok',
      profile: context.catalog.profile.name,
      tools: context.catalog.visible.length,
      hiddenTools: context.catalog.hidden.length,
      api: context.catalog.document,
      authMode: context.config.authMode,
      readOnly: context.config.readOnly,
    });
    return;
  }

  if (path !== '/') {
    respondJson(response, 404, { error: `no route for ${path}` });
    return;
  }

  if (request.method !== 'POST') {
    response.setHeader('allow', 'POST');
    respondJson(response, 405, {
      error: `${request.method} is not supported: this server runs stateless, so it has no server-initiated stream and no session to delete. Send JSON-RPC over POST.`,
    });
    return;
  }

  const server = createMcpServer(context);
  const transport = new StreamableHTTPServerTransport({ sessionIdGenerator: undefined });

  response.on('close', () => {
    void transport.close();
    void server.close();
  });

  await server.connect(transport);
  await transport.handleRequest(request, response);
}

function respondJson(response: ServerResponse, status: number, payload: unknown): void {
  const body = JSON.stringify(payload);
  response.writeHead(status, {
    'content-type': 'application/json',
    'content-length': Buffer.byteLength(body),
  });
  response.end(body);
}
