# Model Transformation Execution Service
FROM gradle:9-jdk21 AS model-transformation-execution-builder

WORKDIR /build
COPY platform/ ./
RUN gradle :model-transformation-execution:shadowJar --no-daemon

# Model Transformation Execution runtime
FROM eclipse-temurin:21-jre-alpine AS model-transformation-execution

WORKDIR /app
COPY --from=model-transformation-execution-builder /build/model-transformation-execution/build/libs/model-transformation-execution.jar ./model-transformation-execution.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "model-transformation-execution.jar"]
