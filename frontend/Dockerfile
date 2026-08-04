FROM node:22-alpine AS build

WORKDIR /usr/src/app

# Native module build deps (python3, make, g++) needed for bufferutil/node-gyp
RUN apk add --no-cache python3 make g++

COPY package*.json ./

RUN npm config set fetch-retry-maxtimeout 6000000 \
    && npm config set fetch-retry-mintimeout 1000000 \
    && npm ci --legacy-peer-deps \
    && apk del python3 make g++

COPY . .

RUN NODE_OPTIONS="--max_old_space_size=4096" npm run build

FROM nginx:1.27.0-alpine
COPY --from=build /usr/src/app/build /usr/share/nginx/html
COPY --from=build /usr/src/app/.env.example /usr/share/nginx/html/.env
COPY --from=build /usr/src/app/nginx-custom.conf /etc/nginx/conf.d/default.conf

RUN apk add --no-cache nodejs npm \
    && npm install -g runtime-env-cra \
    && npm cache clean --force

WORKDIR /usr/share/nginx/html
EXPOSE 3000

CMD ["/bin/sh", "-c", "runtime-env-cra && nginx -g 'daemon off;'"]