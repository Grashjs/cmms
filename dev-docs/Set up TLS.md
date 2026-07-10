# Set up TLS
TLS encrypts traffic between users and Atlas CMMS, enabling secure HTTPS connections. For production deployments, configure one of the options below
## Caddy (simplest, auto-HTTPS)

Caddy automatically provisions and renews Let's Encrypt certificates.

**Caddyfile:**
```
cmms.example.com {
    reverse_proxy localhost:3000
}
```

**Run:**
```bash
docker run -d \
  --name caddy \
  -p 80:80 -p 443:443 \
  -v ./Caddyfile:/etc/caddy/Caddyfile \
  -v caddy_data:/data \
  caddy:2
```

No further configuration needed. Certificates are managed automatically.

---

## Traefik

Add labels to the `nginx` service in `docker-compose.yml` and remove
the published port:

```yaml
  nginx:
    image: nginx:1.27-alpine
    container_name: atlas_nginx
    depends_on:
      - frontend
      - api
      - minio
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.atlas.rule=Host(`cmms.example.com`)"
      - "traefik.http.routers.atlas.tls.certresolver=letsencrypt"
      - "traefik.http.routers.atlas.entrypoints=websecure"
      - "traefik.http.services.atlas.loadbalancer.server.port=80"
```

Remove the `ports` section from the nginx service — Traefik handles
inbound traffic.

---

## NGINX Proxy Manager (GUI)

1. Deploy NGINX Proxy Manager alongside Atlas CMMS.
2. In the admin UI, add a new proxy host:
   - Domain: `cmms.example.com`
   - Forward Hostname / IP: `atlas_nginx`
   - Forward Port: `80`
3. Enable "Block Common Exploits" and "Websockets Support".
4. Under SSL, request a new certificate with Let's Encrypt.

---

## Cloudflare Tunnel

No open inbound ports needed. The tunnel connects outbound to Cloudflare's
edge.

1. Install `cloudflared` on your host.
2. Create a tunnel:
   ```bash
   cloudflared tunnel create atlas-cmms
   ```
3. Add a DNS record pointing `cmms.example.com` to the tunnel.
4. Create a config file `config.yml`:
   ```yaml
   tunnel: <tunnel-id>
   credentials-file: /root/.cloudflared/<tunnel-id>.json

   ingress:
     - hostname: cmms.example.com
       service: http://atlas_nginx:80
     - service: http_status:404
   ```
5. Run the tunnel:
   ```bash
   docker run -d \
     --name cloudflared \
     --network <atlas-network> \
     -v ./config.yml:/etc/cloudflared/config.yml:ro \
     -v /root/.cloudflared:/root/.cloudflared:ro \
     cloudflared tunnel --config /etc/cloudflared/config.yml run
   ```

Cloudflare handles TLS at the edge. Set `PUBLIC_FRONT_URL=https://cmms.example.com`.

---

## Certbot (manual TLS in nginx)

Mount your certificates and uncomment the HTTPS server block in `nginx.conf`:

```bash
mkdir -p certs
# place fullchain.pem and privkey.pem in ./certs
```

In `docker-compose.yml`, add the cert volume to the nginx service:

```yaml
  nginx:
    volumes:
      - ./nginx.conf:/etc/nginx/conf.d/default.conf:ro
      - ./certs:/etc/nginx/certs:ro
    ports:
      - "443:443"
```

Then uncomment the `server` block at the bottom of `nginx.conf` and set
`server_name` to your domain.

For automatic renewal, add a certbot container or cron job.
