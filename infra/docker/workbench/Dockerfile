# Workbench builder
FROM node:22-alpine AS workbench-builder

WORKDIR /build
COPY app/ ./

RUN npm install
RUN npm run build -w @mdeo/workbench

# Workbench runtime (nginx)
FROM nginx:alpine AS workbench

# Copy built assets
COPY --from=workbench-builder /build/packages/workbench/dist /usr/share/nginx/html

# Copy nginx configuration template
COPY infra/docker/workbench/nginx.conf /etc/nginx/nginx.conf.template

EXPOSE 80

# Use environment variable substitution for nginx config
CMD envsubst '${PLUGIN_SERVICE_HOST}' < /etc/nginx/nginx.conf.template > /etc/nginx/nginx.conf && nginx -g 'daemon off;'
