FROM nginx:1.27-alpine

# The reverse proxy config is baked into the image rather than bind-mounted.
# Coolify rewrites relative compose mounts to /data/coolify/applications/<uuid>/
# and Docker silently creates a *directory* there when the file is absent, which
# then fails to mount onto /etc/nginx/conf.d/default.conf. Baking it in also
# means the routing config is versioned and ships with every deploy.
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
