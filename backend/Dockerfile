# syntax=docker/dockerfile:1

# =========================================================
# BUILD
# =========================================================

FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -DskipTests dependency:go-offline

COPY src/main ./src/main

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -Dmaven.test.skip=true package \
    && cp target/backend-*.jar /workspace/app.jar

# =========================================================
# RUNTIME
# =========================================================

FROM eclipse-temurin:25-jre-ubi10-minimal AS runtime

WORKDIR /app

COPY --from=build \
     --chown=10001:0 \
     /workspace/app.jar \
     /app/app.jar

USER 10001:0

ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Duser.timezone=UTC"

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
