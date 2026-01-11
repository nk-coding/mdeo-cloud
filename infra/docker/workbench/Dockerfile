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

# Copy nginx configuration
COPY infra/docker/workbench/nginx.conf /etc/nginx/nginx.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
