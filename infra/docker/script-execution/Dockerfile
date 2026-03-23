# Script Execution Service
FROM gradle:9-jdk21 AS script-execution-builder

WORKDIR /build
COPY platform/ ./
RUN gradle :script-execution:shadowJar --no-daemon

# Script Execution runtime
FROM eclipse-temurin:21-jre-alpine AS script-execution

WORKDIR /app
COPY --from=script-execution-builder /build/script-execution/build/libs/script-execution.jar ./script-execution.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "script-execution.jar"]
