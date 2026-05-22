# Troubleshooting: "Failed to fetch" on Signup

If you see a **"Failed to fetch"** error when trying to sign up on the Atlas CMMS frontend, follow the steps below in order.

---

## Step 1 — Check that the backend container is running

Run the following command on your server:

```bash
docker ps
```

You should see `atlas-cmms-backend` listed with a status of `Up`. If it is missing or shows `Exited`, start it:

```bash
docker-compose up -d
```

If the container keeps crashing, inspect the logs for errors:

```bash
docker logs atlas-cmms-backend
```

Read the error carefully and fix the root cause before continuing (common causes: wrong database credentials, missing environment variables, or a port already in use).

---

## Step 2 — Verify that the required ports are open

Atlas CMMS uses three ports by default:

| Service  | Default Port |
|----------|-------------|
| Frontend | `3000`      |
| Backend  | `8080`      |
| MinIO    | `9000`      |

If you are on a remote server (VPS, cloud instance, etc.), make sure these ports are allowed through your firewall or security group rules. On Linux with `ufw`:

```bash
sudo ufw allow 3000
sudo ufw allow 8080
sudo ufw allow 9000
```

---

## Step 3 — Make sure `PUBLIC_FRONT_URL` matches the URL in your browser

Open your `.env` file and check the value of `PUBLIC_FRONT_URL`:

```env
PUBLIC_FRONT_URL=http://your.server.ip:3000
```

The URL you type in the browser **must exactly match** this value, including the protocol (`http` or `https`) and port number. If they differ, the backend will reject requests due to CORS.

> **Example:** If `PUBLIC_FRONT_URL=http://192.168.1.10:3000`, you must open `http://192.168.1.10:3000` in your browser — not `http://localhost:3000` or any other address.

After any change to `.env`, restart the stack:

```bash
docker-compose down && docker-compose up -d
```

---

## Step 4 — Make sure the frontend and backend use the same protocol

The frontend and backend must both use **HTTP** or both use **HTTPS**. Mixing them (e.g. frontend on `https://` calling a backend on `http://`) will cause browsers to block the request.

Check your `.env`:

```env
PUBLIC_FRONT_URL=https://your.domain.com      # frontend
PUBLIC_API_URL=https://your.domain.com:8080   # backend — must use the same protocol
```

If you are using a reverse proxy (Nginx, Traefik, Caddy) with SSL termination, make sure the proxy forwards requests correctly and that both URLs are `https`.

---

## Step 5 — Still failing? Check the browser developer tools

Open your browser's developer tools (press `F12`) and go to the **Console** or **Network** tab, then try to sign up again.

**Network tab** — click the failed request and look at:
- **Request URL** — is it pointing to the correct backend URL?
- **Status** — is it `CORS error`, `net::ERR_CONNECTION_REFUSED`, `net::ERR_SSL_PROTOCOL_ERROR`, etc.?

**Console tab** — look for red error messages such as:
- `Access to fetch ... has been blocked by CORS policy` → recheck Steps 3 and 4, and make sure `ENABLE_CORS=true` is set in `.env`.
- `net::ERR_CONNECTION_REFUSED` → the backend is not reachable; recheck Steps 1 and 2.
- `net::ERR_SSL_PROTOCOL_ERROR` → protocol mismatch; recheck Step 4.

Share the error message in the [Discord server](https://discord.gg/cHqyVRYpkA) or open an issue on GitHub if you need further help.