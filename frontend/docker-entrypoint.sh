#!/bin/sh
set -e

# runtime-env-cra reads the key list from ./.env and looks every key up in
# process.env, throwing when a value is unset *or* empty. Upstream keeps the
# optional keys truthy through docker-compose defaults of a single space
# ("${GOOGLE_TRACKING_ID:- }"), which works with plain `docker compose` but not
# behind Coolify: it resolves the environment itself and the spaces do not
# survive, so the container crash-loops on the first blank key.
#
# For every blank key, fall back to the default baked into .env — and only to a
# space when that default is empty too, so booleans stay "false" instead of
# silently becoming a truthy blank. This keeps the upstream contract intact
# without depending on how the orchestrator passes empty values.
ENV_FILE=/usr/share/nginx/html/.env

# A CRLF checkout would leave a stray \r on every value — which reads as
# "non-empty" and would be baked into runtime-env.js verbatim.
CR=$(printf '\r')

while IFS= read -r line || [ -n "$line" ]; do
  line=${line%"$CR"}
  case "$line" in '' | \#*) continue ;; esac
  key=${line%%=*}
  case "$key" in '' | *[!A-Za-z0-9_]*) continue ;; esac
  if [ -n "$(printenv "$key" 2>/dev/null)" ]; then
    continue
  fi
  fallback=${line#*=}
  [ -n "$fallback" ] || fallback=" "
  export "$key=$fallback"
done < "$ENV_FILE"

runtime-env-cra
exec nginx -g "daemon off;"
