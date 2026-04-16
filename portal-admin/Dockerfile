# ============================================================================
# Sannad portal — React + Vite static build served by nginx
#
# Build accepts VITE_API_URL so axios knows where the backend lives
# (default: empty, which means relative URLs — use that when nginx
# is configured to proxy /admin + /api to the backend).
#
# On Railway the most reliable setup is:
#   VITE_API_URL = https://<backend-service>.up.railway.app
# and CORS enabled on the backend for this origin.
# ============================================================================

FROM node:22-alpine AS builder
WORKDIR /build

COPY package.json package-lock.json* ./
RUN npm ci --no-audit --no-fund

COPY . .

# Bake the backend URL into the bundle at build time.
ARG VITE_API_URL=""
ENV VITE_API_URL=${VITE_API_URL}

RUN npm run build

# -----------------------------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

# Remove default nginx config and install our SPA config
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/sannad.conf

COPY --from=builder /build/dist /usr/share/nginx/html

# Railway injects PORT — rewrite nginx listen directive at container start.
# The template uses $PORT so we substitute it before nginx runs.
RUN sed -i 's/listen 8080;/listen ${PORT:-8080};/' /etc/nginx/conf.d/sannad.conf

EXPOSE 8080
CMD ["/bin/sh", "-c", "envsubst '$PORT' < /etc/nginx/conf.d/sannad.conf > /etc/nginx/conf.d/default.conf && rm /etc/nginx/conf.d/sannad.conf && nginx -g 'daemon off;'"]
