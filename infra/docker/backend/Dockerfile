# Backend
FROM gradle:9-jdk21 AS backend-builder

WORKDIR /build
COPY platform/ ./
RUN gradle :backend:shadowJar --no-daemon

# Backend runtime
FROM eclipse-temurin:21-jre-alpine AS backend

WORKDIR /app
COPY --from=backend-builder /build/backend/build/libs/backend.jar ./backend.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "backend.jar"]
