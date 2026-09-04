/**
 * The slice of OpenAPI this server actually reads. Deliberately not a full model of the
 * specification: everything below is what `springdoc` emits for this API (OpenAPI 3.1,
 * `springdoc.api-docs.version: openapi_3_1` in application.yml), and reading less means
 * fewer assumptions that can break on the next upstream sync.
 */

export type JsonSchema = Record<string, unknown>;

export interface OpenApiParameter {
  name: string;
  in: 'path' | 'query' | 'header' | 'cookie';
  description?: string;
  required?: boolean;
  schema?: JsonSchema;
}

export interface OpenApiRequestBody {
  description?: string;
  required?: boolean;
  content?: Record<string, { schema?: JsonSchema }>;
}

export interface OpenApiResponse {
  description?: string;
  content?: Record<string, { schema?: JsonSchema }>;
}

export interface OpenApiOperation {
  operationId?: string;
  summary?: string;
  description?: string;
  tags?: string[];
  deprecated?: boolean;
  parameters?: OpenApiParameter[];
  requestBody?: OpenApiRequestBody;
  responses?: Record<string, OpenApiResponse>;
}

export type PathItem = Record<string, OpenApiOperation | OpenApiParameter[] | unknown>;

export interface OpenApiDocument {
  openapi?: string;
  info?: { title?: string; version?: string; description?: string };
  servers?: { url: string; description?: string }[];
  paths?: Record<string, PathItem>;
  components?: {
    schemas?: Record<string, JsonSchema>;
    securitySchemes?: Record<string, JsonSchema>;
  };
}

export const HTTP_METHODS = ['get', 'post', 'put', 'patch', 'delete'] as const;

export type HttpMethod = (typeof HTTP_METHODS)[number];

export function isHttpMethod(value: string): value is HttpMethod {
  return (HTTP_METHODS as readonly string[]).includes(value);
}
