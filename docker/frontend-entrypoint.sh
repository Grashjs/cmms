#!/bin/sh
set -e

cd /usr/share/nginx/html

# runtime-env-cra rejects empty process.env values in production mode.
# Generate runtime-env.js directly so optional vars (GOOGLE_KEY, LOGO_PATHS, etc.) can be blank.
node <<'EOF'
const fs = require('fs');

let envFile = '';
try {
  envFile = fs.readFileSync('.env', 'utf8');
} catch (err) {
  if (err.code !== 'ENOENT') throw err;
}
const config = {};

for (const rawLine of envFile.split(/\r?\n/)) {
  const line = rawLine.split('#')[0].trim();
  if (!line || !line.includes('=')) continue;

  const eq = line.indexOf('=');
  const key = line.slice(0, eq);
  const defaultValue = line.slice(eq + 1);

  config[key] = Object.prototype.hasOwnProperty.call(process.env, key)
    ? process.env[key]
    : defaultValue;
}

fs.writeFileSync(
  'runtime-env.js',
  `window.__RUNTIME_CONFIG__ = ${JSON.stringify(config)};`
);
EOF

exec nginx -g 'daemon off;'
