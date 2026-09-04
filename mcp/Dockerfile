# cmms4fm-mcp — MCP server over the CMMS REST API.
#
# The major is pinned, the patch is not. `package.json` declares `engines: >=22.12.0`, and
# npm only *warns* on an engines mismatch instead of refusing to install — which is how the
# frontend image once shipped a Node that satisfied nothing (see CLAUDE.md, "The builder
# image's Node version is coupled to Vite"). Pinning the major is the constraint that
# actually matters here; pinning a patch that later disappears from the registry is not.
FROM node:22-alpine AS build

WORKDIR /app

# Copied on their own so a source change does not invalidate the dependency layer.
COPY package.json package-lock.json ./
RUN npm ci

COPY tsconfig.json ./
COPY src ./src
# The compiler is the check: `tsc` with `strict` on, so a type error fails the image build
# rather than the container. Tests are not in the image — CI runs them separately.
RUN npx tsc && npm prune --omit=dev

FROM node:22-alpine

ENV NODE_ENV=production
WORKDIR /app

COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY package.json ./

# The base image ships an unprivileged `node` user. The server needs no filesystem access
# beyond its own code, and nothing here writes.
USER node

EXPOSE 8081

# Ready means the OpenAPI document is loaded, which is exactly what /healthz answers.
# wget comes from busybox in the alpine base.
HEALTHCHECK --interval=10s --timeout=5s --retries=12 --start-period=60s \
  CMD wget -qO- http://127.0.0.1:8081/healthz | grep -q '"status":"ok"'

CMD ["node", "dist/src/index.js"]
