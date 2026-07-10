# Single-Domain Deployment (Reverse Proxy)

This adds an optional nginx container that puts the frontend, backend, and
MinIO behind **one public URL**. It's fully additive — the existing `docker-compose.yml` still works
unchanged for anyone who doesn't want this.

## What's new

- `nginx/nginx.conf` — routes `/` → frontend, `/api/` → backend, `/storage/` → MinIO
- `docker-compose.proxy.yml` — same services as the default compose file, plus
  the nginx container; `api`, `frontend`, and `minio` no longer publish ports
  to the host (nginx is the only public entrypoint)

## Quick start

```bash
# from the repo root
cp .env.example .env
# edit .env: set PUBLIC_FRONT_URL=http://localhost (or your real domain)

docker compose -f docker-compose.proxy.yml up -d
```

Visit `http://localhost` (or your domain). That's it — no separate ports for
the API or MinIO, no CORS configuration.

## What changed under the hood

| Before | After |
|---|---|
| `cmms.example.com:3000` (frontend) | `cmms.example.com` |
| `cmms.example.com:8080` (API) | `cmms.example.com/api` |
| `cmms.example.com:9000` (MinIO) | `cmms.example.com/storage` |
| `ENABLE_CORS=true` | `ENABLE_CORS=false` (same-origin, not needed) |
| `PUBLIC_API_URL=http://ip:8080` | `PUBLIC_API_URL=${PUBLIC_FRONT_URL}/api` |
| `PUBLIC_MINIO_ENDPOINT=http://ip:9000` | `PUBLIC_MINIO_ENDPOINT=${PUBLIC_FRONT_URL}/storage` |
| Frontend `API_URL=http://ip:8080` | Frontend `API_URL=/api` (relative) |

Only one `.env` variable needs to be correct now: `PUBLIC_FRONT_URL`.

## Production HTTPS

Most self-hosters already run a TLS-terminating proxy in front of their
Docker host. The nginx container here stays on plain HTTP internally and
sits behind whichever of these you already use:

**Caddy** (simplest, auto-HTTPS):
```
cmms.example.com {
    reverse_proxy localhost:80
}
```

**Traefik** — add labels to the `nginx` service instead of publishing port 80:
```yaml
  nginx:
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.atlas.rule=Host(`cmms.example.com`)"
      - "traefik.http.routers.atlas.tls.certresolver=letsencrypt"
```

**Cloudflare Tunnel** — point the tunnel's public hostname at
`http://nginx:80` (or `http://localhost:80`), no open inbound ports needed
at all.

**Certbot directly in the nginx container** — the commented-out `server`
block at the bottom of `nginx/nginx.conf` shows the 443 config; mount your
`fullchain.pem`/`privkey.pem` into `./certs` and uncomment it.

## Migration notes for existing self-hosted users

This is opt-in and non-breaking:

- **Existing deployments**: keep using `docker-compose.yml` and your current
  `PUBLIC_API_URL` / `PUBLIC_MINIO_ENDPOINT` — nothing changes for you.
- **New deployments**: use `docker-compose.proxy.yml` and only set
  `PUBLIC_FRONT_URL`.
- **Switching an existing deployment over**: back up first. Update `.env` as
  shown in the table above, then run
  `docker compose -f docker-compose.proxy.yml up -d`. Existing data in the
  `postgres_data` and `minio_data` volumes is unaffected since volume names
  are unchanged.

## Troubleshooting

- **"Failed to fetch" on file uploads**: check `client_max_body_size` in
  `nginx.conf` matches or exceeds your largest expected attachment.
- **Broken images/attachments after switching**: `PUBLIC_MINIO_ENDPOINT` must
  match exactly what's used in already-generated presigned URLs' host —
  new uploads will use the new value automatically; only very old cached
  links could be affected.
- **SSO redirect URI**: if using `ENABLE_SSO`, update your OAuth2 provider's
  redirect URI to `${PUBLIC_FRONT_URL}/api/oauth2/callback/${OAUTH2_PROVIDER}`.
