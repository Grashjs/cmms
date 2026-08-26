FROM node:21.6.1 AS build

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

