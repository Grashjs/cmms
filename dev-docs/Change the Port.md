
# Changing Ports in Atlas CMMS Docker Setup

With the nginx reverse proxy, all traffic enters through a single port.

| Service | Default Port | Description        |
|---------|--------------|--------------------|
| nginx   | 3000            | Reverse proxy entrypoint |

Internal services (frontend, API, MinIO, PostgreSQL) are not exposed to the host.

---

## How to Change the Port

1. **Open `docker-compose.yml`**

   Locate the `ports` section under the `nginx` service.

2. **Modify the host port**

   Docker uses the format `host_port:container_port`.
   Update the **host port** to your desired value.

From

 ```yaml
   nginx:
     ports:
       - "3000:80" # host:container
   ```
to
   ```yaml
   nginx:
     ports:
       - "8080:80" # host:container
   ```

3. **Restart**

   ```bash
   docker compose down
   docker compose up -d
   ```

   Then open `http://localhost:8080`.

---

## Common Pitfalls

- Ensure no other services are already using the new port.
- If using Traefik or Caddy in front of nginx, update its config to point at the new port.
