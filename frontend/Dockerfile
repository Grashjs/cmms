# Node 22, not 21: vite 8 declares engines.node "^20.19.0 || >=22.12.0", which Node 21
# satisfies neither. npm only warns about that, so the mismatch does not surface at
# install time - it surfaces when the bundler runs, in CI, after a green local build on
# whatever newer Node the developer happens to have. When bumping vite, check this line.
FROM node:22.12.0 AS build

WORKDIR /usr/src/app
COPY package*.json ./
RUN npm config set fetch-retry-maxtimeout 6000000
RUN npm config set fetch-retry-mintimeout 1000000
RUN npm install --legacy-peer-deps
COPY . .
# The heap ceiling is a leftover from the CRA/webpack build, which needed 2-4 GB.
# Vite builds this in about a minute and well under that, but the ceiling costs
# nothing and keeps a constrained builder from being the thing that fails.
RUN NODE_OPTIONS="--max-old-space-size=4096" npm run build

FROM nginx:1.27.0-alpine
COPY --from=build /usr/src/app/build /usr/share/nginx/html
COPY --from=build /usr/src/app/.env.example /usr/share/nginx/html/.env
COPY --from=build /usr/src/app/nginx-custom.conf /etc/nginx/conf.d/default.conf

RUN apk add --update nodejs
RUN apk add --update npm

RUN npm i -g runtime-env-cra

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

WORKDIR /usr/share/nginx/html
EXPOSE 3000

CMD ["/bin/sh", "/docker-entrypoint.sh"]

