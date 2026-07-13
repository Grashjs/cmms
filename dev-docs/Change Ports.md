
# 🔧 Changing Ports in Atlas CMMS Docker Setup

By default, the Atlas CMMS system uses the following ports:

| Service    | Default Port | Description               |
|------------|--------------|---------------------------|
| PostgreSQL | 5432         | Database server           |
| Backend    | 8080         | Spring Boot API           |
| Frontend   | 3000         | React Web App             |
| MinIO      | 9000, 9001   | Object storage + console  |

You can change these ports to avoid conflicts with other services on your machine.

---

## ⚙️ Step-by-Step: How to Change Ports

1. **Open `docker-compose.yml`**

   Locate the `ports` section under each service.

2. **Modify the Left-Hand Side of the Port Mapping**

   Docker uses the format `host_port:container_port`.  
   To change the port exposed on your local machine, update the **host port**.

   ```yaml
   # Example: Change backend port from 8080 to 9090
   api:
     ports:
       - "9090:8080" # host:container
   ```

3. **Update Related Environment Variables (If Needed)**

   If other services or your `.env` file refer to the old port (e.g., `PUBLIC_API_URL=http://localhost:8080`), update it to match the new port:

   ```env
   PUBLIC_API_URL=http://localhost:9090
   ```

4. **Optional: Change Frontend Port**

   ```yaml
   frontend:
     ports:
       - "4000:3000"
   ```

   And update your browser URL to: `http://localhost:4000`

5. **Optional: Change PostgreSQL or MinIO Ports**

   ```yaml
   postgres:
     ports:
       - "55432:5432"

   minio:
     ports:
       - "19000:9000"
       - "19001:9001"
   ```

---

## 🧪 Test It Out

After updating the ports:

```bash
docker compose down
docker compose up -d
```

Then open the frontend in your browser (based on the new port) and verify everything works.

---

## ❗ Common Pitfalls

- Always update `.env` variables to match new ports where necessary.
- Ensure no other services are already using the new ports you choose.
- If using Traefik or Nginx as a reverse proxy, make sure routing reflects port changes.
