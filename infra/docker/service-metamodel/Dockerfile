# Build stage
FROM node:23-alpine AS builder

WORKDIR /app

# Copy package files
COPY app/package*.json ./
COPY app/tsconfig*.json ./
COPY app/packages ./packages

# Install dependencies
RUN npm ci

# Build the packages
RUN npm run build

# Production stage
FROM node:23-alpine

# Install curl for healthchecks
RUN apk add --no-cache curl

WORKDIR /workspace

# Copy the entire packages structure with built artifacts and dependencies
COPY --from=builder /app/packages /workspace/packages
COPY --from=builder /app/node_modules /workspace/node_modules
COPY --from=builder /app/package.json /workspace/

# Set working directory to service-metamodel
WORKDIR /workspace/packages/service-metamodel

# Set environment variables
ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0

EXPOSE 3000

CMD ["node", "dist/index.js"]
