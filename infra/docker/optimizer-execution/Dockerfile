# Optimizer Execution Service
FROM gradle:9-jdk21 AS optimizer-execution-builder

WORKDIR /build
COPY platform/ ./
RUN gradle :optimizer-execution:shadowJar --no-daemon

# Optimizer Execution runtime
FROM eclipse-temurin:21-jre-alpine AS optimizer-execution

WORKDIR /app
COPY --from=optimizer-execution-builder /build/optimizer-execution/build/libs/optimizer-execution.jar ./optimizer-execution.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", \
    "--add-opens", "java.xml/org.xml.sax.helpers=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang=ALL-UNNAMED", \
    "--add-opens", "java.base/java.util=ALL-UNNAMED", \
    "--add-opens", "java.base/java.lang.reflect=ALL-UNNAMED", \
    "-jar", "optimizer-execution.jar"]
