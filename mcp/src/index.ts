import { loadConfig } from './config.js';
import { createLogger } from './logger.js';
import { loadSpecWithRetry } from './openapi/loader.js';
import { RateLimiter } from './ratelimit.js';
import type { ServerContext } from './server.js';
import { buildCatalog } from './tools/registry.js';
import { startHttpTransport } from './transport/http.js';
import { startStdioTransport } from './transport/stdio.js';

/**
 * Boot order matters and is short: read the environment, load the OpenAPI document, derive
 * the tool catalogue, then start a transport. The document is loaded *before* anything is
 * served, because "ready" means "the document is loaded" — a server that answered
 * `tools/list` with an empty set while still fetching would look healthy and be useless.
 */
async function main(): Promise<void> {
  const config = loadConfig();
  const logger = createLogger(config);

  const spec = await loadSpecWithRetry(config, logger);
  const catalog = buildCatalog(spec.document, config, logger);

  logger.info('tool catalogue built', {
    profile: catalog.profile.name,
    visible: catalog.visible.length,
    hidden: catalog.hidden.length,
    excluded: catalog.excluded.length,
    curated: catalog.visible.filter((tool) => tool.curated).length,
    readOnly: config.readOnly,
    authMode: config.authMode,
  });

  if (catalog.visible.length === 0) {
    // Never a working state, always a configuration mistake — most likely PROFILE plus
    // READ_ONLY excluding each other, or a TOOLS_DENY that swallowed everything.
    logger.error('no tools are visible: check PROFILE, READ_ONLY, TOOLS_ALLOW and TOOLS_DENY', {
      profile: config.profile,
      readOnly: config.readOnly,
      toolsAllow: config.toolsAllow,
      toolsDeny: config.toolsDeny,
    });
  }

  const context: ServerContext = {
    config,
    logger,
    catalog,
    document: spec.document,
    rateLimiter: new RateLimiter({
      perMinute: config.rateLimitPerMinute,
      burst: config.rateLimitBurst,
    }),
  };

  if (config.transport === 'stdio') {
    await startStdioTransport(context);
  } else {
    const handle = await startHttpTransport(context);
    const shutdown = (signal: string) => {
      logger.info('shutting down', { signal });
      void handle.close().then(() => process.exit(0));
    };
    process.on('SIGTERM', () => shutdown('SIGTERM'));
    process.on('SIGINT', () => shutdown('SIGINT'));
  }

  if (config.specRefreshMinutes > 0) {
    scheduleSpecRefresh(context);
  }
}

/**
 * Re-reads the document on an interval so an endpoint added upstream becomes a tool without
 * a redeploy. A failed refresh keeps the previous catalogue: a momentarily unreachable API
 * must not empty the tool set.
 */
function scheduleSpecRefresh(context: ServerContext): void {
  const { config, logger } = context;
  const interval = setInterval(
    () => {
      void (async () => {
        try {
          const spec = await loadSpecWithRetry(config, logger, 1, 0);
          const next = buildCatalog(spec.document, config, logger);
          const before = context.catalog.visible.map((tool) => tool.name).join(',');
          const after = next.visible.map((tool) => tool.name).join(',');
          context.catalog = next;
          context.document = spec.document;
          if (before !== after) {
            logger.info('tool set changed after refreshing the OpenAPI document', {
              visible: next.visible.length,
            });
          }
        } catch (error) {
          logger.warn('refreshing the OpenAPI document failed, keeping the previous catalogue', {
            error: error instanceof Error ? error.message : String(error),
          });
        }
      })();
    },
    config.specRefreshMinutes * 60_000,
  );
  interval.unref();
}

main().catch((error: unknown) => {
  // Nothing is serving yet at this point, so this is the only place a failure can be
  // reported. stderr, not stdout: on stdio, stdout is the protocol.
  process.stderr.write(
    `${JSON.stringify({
      ts: new Date().toISOString(),
      level: 'error',
      message: 'cmms4fm-mcp failed to start',
      error: error instanceof Error ? error.message : String(error),
    })}\n`,
  );
  process.exit(1);
});
