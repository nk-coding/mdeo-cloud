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

WORKDIR /workspace

# Copy the entire packages structure with built artifacts and dependencies
COPY --from=builder /app/packages /workspace/packages
COPY --from=builder /app/node_modules /workspace/node_modules
COPY --from=builder /app/package.json /workspace/

# Set working directory to service-model
WORKDIR /workspace/packages/service-model

# Set environment variables
ENV NODE_ENV=production
ENV PORT=3001
ENV HOST=0.0.0.0

EXPOSE 3001

CMD ["node", "dist/index.js"]
