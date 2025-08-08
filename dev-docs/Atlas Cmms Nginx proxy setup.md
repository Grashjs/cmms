# Atlas CMMS NGINX proxy manager setup guide

## 1. Subdomain Setup

You will need **three subdomains** pointed at your NGINX Proxy Manager (NPM) installation.  
Example subdomains:

- `maint.domain.com` → Access your Atlas CMMS instance
- `maintapi.domain.com` → Backend API (used by frontend)
- `maintminio.domain.com` → Access for MinIO containers

## 2. Proxy Hosts Setup

Assume your server IP is `192.168.1.223` and you're using default ports.

Create **three proxy hosts** in NGINX Proxy Manager (one per subdomain/port).

### Configuration:

- **Details Tab:**
  - Enable **WebSocket support**
  - Enable **Block common exploits**
- **SSL Tab:**
  - Enable everything **except HSTS Subdomains**
    - (This showed no benefit in testing)

## 3. Expected Outcome

You should now have **three proxy hosts** set up — one for each service.

## 4. Configure `.env` File

Set the following variables in your `.env` file:

```env
PUBLIC_FRONT_URL=https://maint.domain.com
PUBLIC_API_URL=https://maintapi.domain.com
PUBLIC_MINIO_ENDPOINT=https://maintminio.domain.com
```

## 5. Restart

Restart your Docker containers or restart the entire Atlas CMMS host/VM.

## 6. Test Access

Access `https://maint.domain.com` in your browser.  
You should **not see any CORS errors**, and the frontend should be able to connect to the backend and MinIO.

---

### 📝 Note:

If you're on a **local network** and your modem **doesn't support loopback**, or you have **custom DNS routing**, you will need to configure those domains to resolve to your NGINX server **manually**.

> Example: If your ISP or modem blocks loopback, tools like **Pi-hole** can help resolve domains locally.