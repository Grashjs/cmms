import {
  HTTP_METHODS,
  isHttpMethod,
  type HttpMethod,
  type JsonSchema,
  type OpenApiDocument,
  type OpenApiOperation,
  type OpenApiParameter,
} from './types.js';

export interface OperationBody {
  contentType: string;
  schema: JsonSchema;
  required: boolean;
}

export interface Operation {
  method: HttpMethod;
  /** Template path as written in the document, e.g. `/assets/{id}`. */
  path: string;
  /** First tag, or `Other`. Used for grouping and for the synthesised description. */
  tag: string;
  /**
   * The document's own `operationId`. Recorded for traceability but **never** used as a
   * tool name: springdoc derives these from the Java method name and disambiguates
   * collisions with a scan-order counter, so `/assets/search` is `search_15` and adding an
   * unrelated controller upstream renumbers it. See tools/naming.ts.
   */
  operationId: string | undefined;
  summary: string | undefined;
  description: string | undefined;
  deprecated: boolean;
  pathParams: OpenApiParameter[];
  queryParams: OpenApiParameter[];
  body: OperationBody | undefined;
  /** Name of the 2xx response schema, when it is a plain `$ref`. Helps describe the tool. */
  responseSchema: string | undefined;
  /** Set when the operation cannot be expressed as an MCP tool; the reason is the value. */
  unsupported: string | undefined;
}

const REF_PREFIX = '#/components/schemas/';

export function extractOperations(document: OpenApiDocument): Operation[] {
  const operations: Operation[] = [];
  const paths = document.paths ?? {};

  for (const path of Object.keys(paths).sort()) {
    const item = paths[path];
    if (!item || typeof item !== 'object') continue;
    // Parameters may sit on the path item and apply to every method under it.
    const shared = asParameters((item as Record<string, unknown>).parameters);

    for (const method of HTTP_METHODS) {
      const candidate = (item as Record<string, unknown>)[method];
      if (!candidate || typeof candidate !== 'object') continue;
      if (!isHttpMethod(method)) continue;
      operations.push(toOperation(method, path, candidate as OpenApiOperation, shared));
    }
  }

  return operations;
}

function asParameters(value: unknown): OpenApiParameter[] {
  return Array.isArray(value) ? (value as OpenApiParameter[]) : [];
}

function toOperation(
  method: HttpMethod,
  path: string,
  operation: OpenApiOperation,
  shared: OpenApiParameter[],
): Operation {
  const parameters = [...shared, ...(operation.parameters ?? [])];
  const pathParams = parameters.filter((parameter) => parameter.in === 'path');
  const queryParams = parameters.filter((parameter) => parameter.in === 'query');

  return {
    method,
    path,
    tag: operation.tags?.[0] ?? 'Other',
    operationId: operation.operationId,
    summary: operation.summary,
    description: operation.description,
    deprecated: operation.deprecated === true,
    pathParams,
    queryParams,
    body: pickBody(operation),
    responseSchema: pickResponseSchema(operation),
    unsupported: findUnsupportedReason(parameters, operation),
  };
}

function pickBody(operation: OpenApiOperation): OperationBody | undefined {
  const content = operation.requestBody?.content;
  if (!content) return undefined;
  const json = content['application/json'] ?? content['application/*+json'];
  if (!json?.schema) return undefined;
  return {
    contentType: 'application/json',
    schema: json.schema,
    required: operation.requestBody?.required === true,
  };
}

function pickResponseSchema(operation: OpenApiOperation): string | undefined {
  const responses = operation.responses ?? {};
  for (const status of ['200', '201', '202', 'default']) {
    const response = responses[status];
    if (!response?.content) continue;
    for (const media of Object.values(response.content)) {
      const ref = media?.schema?.$ref;
      if (typeof ref === 'string' && ref.startsWith(REF_PREFIX)) {
        return ref.slice(REF_PREFIX.length);
      }
      const items = media?.schema?.items as JsonSchema | undefined;
      const itemRef = items?.$ref;
      if (typeof itemRef === 'string' && itemRef.startsWith(REF_PREFIX)) {
        return `${itemRef.slice(REF_PREFIX.length)}[]`;
      }
    }
  }
  return undefined;
}

/**
 * File transfer cannot travel through a JSON tool call, and pretending otherwise produces a
 * tool that always fails. Two operations in this document take binary uploads —
 * `POST /files/upload` and `POST /files/upload/request-portal/{uuid}`, both of which
 * declare the binary array as a *query* parameter — so the check looks at parameters, not
 * just at the request body. A request body in a media type other than JSON is excluded for
 * the same reason.
 */
function findUnsupportedReason(
  parameters: OpenApiParameter[],
  operation: OpenApiOperation,
): string | undefined {
  for (const parameter of parameters) {
    if (hasBinaryFormat(parameter.schema)) {
      return `parameter "${parameter.name}" carries binary content, which a JSON tool call cannot express`;
    }
  }
  const content = operation.requestBody?.content;
  if (content && Object.keys(content).length > 0) {
    const mediaTypes = Object.keys(content);
    const hasJson = mediaTypes.some((type) => type === 'application/json' || type.endsWith('+json'));
    if (!hasJson) {
      return `request body is ${mediaTypes.join(', ')}, not JSON`;
    }
  }
  return undefined;
}

function hasBinaryFormat(schema: JsonSchema | undefined): boolean {
  if (!schema) return false;
  if (schema.format === 'binary') return true;
  const items = schema.items as JsonSchema | undefined;
  return hasBinaryFormat(items);
}
