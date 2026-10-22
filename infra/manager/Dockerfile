# mdeo-manager: the web manager of a docker compose deployment of mdeo (infra/manager/README.md).
# Build context: the repository root (as for every image of docker-publish.yml).
FROM golang:1.25.14-alpine AS build
WORKDIR /src
COPY infra/manager/ ./
ARG SERVICE_VERSION=""
ENV CGO_ENABLED=0
RUN go vet ./... && go test -count=1 ./... \
    && go build -trimpath -ldflags "-s -w -X main.version=${SERVICE_VERSION:-dev}" -o /mdeo-manager .

# The docker CLI with its compose plugin; the manager drives the host's daemon through the
# mounted socket.
FROM docker:28.5.2-cli
COPY --from=build /mdeo-manager /usr/local/bin/mdeo-manager
ENV MANAGER_LISTEN=:8090 \
    STATE_DIR=/state \
    STACK_ENV_FILE=/config/stack.env
EXPOSE 8090
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 CMD ["mdeo-manager", "healthcheck"]
ENTRYPOINT ["mdeo-manager"]
CMD ["serve"]
