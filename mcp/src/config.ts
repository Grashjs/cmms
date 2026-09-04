/**
 * Environment is the only configuration surface. There is no config file and no
 * database: everything this server knows comes from the OpenAPI document it loads at
 * boot plus the variables below. See docs/mcp-server-konzept.md §6.
 */

export type Transport = 'http' | 'stdio';
export type AuthMode = 'passthrough' | 'service';

export interface Config {
  /** Base URL of the CMMS REST API, without a trailing slash. `/assets/1` is appended to it. */
  cmmsBaseUrl: string;
  /**
   * Where the OpenAPI document lives. Defaults to the *group* URL, not `/v3/api-docs`:
   * `springdoc.enable-default-api-docs` is false in application.yml, so only named groups
   * are served and the plain path answers 404.
   */
  specUrl: string;
  /** Optional local copy of the document, used when the URL cannot be reached. */
  specFile: string | undefined;
  /** Re-read the document every N minutes; 0 disables it. A changed tool set is announced. */
  specRefreshMinutes: number;
  transport: Transport;
  port: number;
  host: string;
  authMode: AuthMode;
  /** Only used with `authMode: 'service'`. */
  serviceApiKey: string | undefined;
  /** Only used with `transport: 'stdio'` and `authMode: 'passthrough'`. */
  stdioApiKey: string | undefined;
  profile: string;
  /** Hides every writing tool regardless of profile. */
  readOnly: boolean;
  /** Extra tool-name globs to allow on top of the profile. */
  toolsAllow: string[];
  /** Tool-name globs to remove, applied last — a deny always wins. */
  toolsDeny: string[];
  /** Sustained tool calls per minute, per API key (falling back to the peer address). */
  rateLimitPerMinute: number;
  /** Burst allowance; defaults to the per-minute rate. */
  rateLimitBurst: number;
  /** How deep `$ref` chains are inlined into a tool's input schema before being cut off. */
  maxSchemaDepth: number;
  /** Input schemas larger than this are pruned to their top level. */
  maxSchemaChars: number;
  /** Responses longer than this are truncated with a marker. */
  maxResponseChars: number;
  requestTimeoutMs: number;
  /** Serve MCP resources (enums, schemas, capability catalogue). */
  enableResources: boolean;
  /** Serve MCP prompts (use-case templates). */
  enablePrompts: boolean;
  logLevel: 'debug' | 'info' | 'warn' | 'error';
}

interface Env {
  str(name: string, fallback?: string): string | undefined;
  bool(name: string, fallback: boolean): boolean;
  int(name: string, fallback: number): number;
  list(name: string): string[];
}

function reader(env: NodeJS.ProcessEnv): Env {
  const str = (name: string, fallback?: string): string | undefined => {
    const raw = env[name];
    if (raw === undefined) return fallback;
    const trimmed = raw.trim();
    // Coolify does not pass empty values through as compose defaults (CLAUDE.md), and the
    // frontend entrypoint's workaround fills blanks with a single space. Treat both as unset.
    return trimmed === '' ? fallback : trimmed;
  };
  return {
    str,
    bool(name, fallback) {
      const raw = str(name);
      if (raw === undefined) return fallback;
      return ['1', 'true', 'yes', 'on'].includes(raw.toLowerCase());
    },
    int(name, fallback) {
      const raw = str(name);
      if (raw === undefined) return fallback;
      const parsed = Number.parseInt(raw, 10);
      if (!Number.isFinite(parsed)) {
        throw new Error(`${name} must be an integer, got ${JSON.stringify(raw)}`);
      }
      return parsed;
    },
    list(name) {
      const raw = str(name);
      if (raw === undefined) return [];
      return raw
        .split(',')
        .map((entry) => entry.trim())
        .filter((entry) => entry.length > 0);
    },
  };
}

export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  const { str, bool, int, list } = reader(env);

  const transport = str('MCP_TRANSPORT', 'http') as Transport;
  if (transport !== 'http' && transport !== 'stdio') {
    throw new Error(`MCP_TRANSPORT must be "http" or "stdio", got ${JSON.stringify(transport)}`);
  }

  const baseUrl = str('CMMS_BASE_URL');
  if (!baseUrl) {
    throw new Error(
      'CMMS_BASE_URL is required (e.g. http://api:8080 or https://cmms.example.com/api)',
    );
  }
  const cmmsBaseUrl = baseUrl.replace(/\/+$/, '');

  const authMode = str('AUTH_MODE', 'passthrough') as AuthMode;
  if (authMode !== 'passthrough' && authMode !== 'service') {
    throw new Error(`AUTH_MODE must be "passthrough" or "service", got ${JSON.stringify(authMode)}`);
  }
  const serviceApiKey = str('SERVICE_API_KEY');
  if (authMode === 'service' && !serviceApiKey) {
    throw new Error('AUTH_MODE=service requires SERVICE_API_KEY');
  }

  const logLevel = str('LOG_LEVEL', 'info') as Config['logLevel'];
  if (!['debug', 'info', 'warn', 'error'].includes(logLevel)) {
    throw new Error(`LOG_LEVEL must be debug|info|warn|error, got ${JSON.stringify(logLevel)}`);
  }

  const rateLimitPerMinute = int('RATE_LIMIT', 120);
  const specGroup = str('SPEC_GROUP', 'atlas-cmms')!;

  return {
    cmmsBaseUrl,
    specUrl: str('SPEC_URL', `${cmmsBaseUrl}/v3/api-docs/${specGroup}`)!,
    specFile: str('SPEC_FILE'),
    specRefreshMinutes: int('SPEC_REFRESH_MINUTES', 0),
    transport,
    port: int('PORT', 8081),
    host: str('HOST', '0.0.0.0')!,
    authMode,
    serviceApiKey,
    stdioApiKey: str('CMMS_API_KEY'),
    profile: str('PROFILE', 'readonly')!,
    readOnly: bool('READ_ONLY', false),
    toolsAllow: list('TOOLS_ALLOW'),
    toolsDeny: list('TOOLS_DENY'),
    rateLimitPerMinute,
    rateLimitBurst: int('RATE_LIMIT_BURST', rateLimitPerMinute),
    maxSchemaDepth: int('MAX_SCHEMA_DEPTH', 6),
    maxSchemaChars: int('MAX_SCHEMA_CHARS', 12000),
    maxResponseChars: int('MAX_RESPONSE_CHARS', 60000),
    requestTimeoutMs: int('REQUEST_TIMEOUT_MS', 30000),
    enableResources: bool('ENABLE_RESOURCES', true),
    enablePrompts: bool('ENABLE_PROMPTS', true),
    logLevel,
  };
}
