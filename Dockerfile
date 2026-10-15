# syntax=docker/dockerfile:1.7
# ---- Build stage ---------------------------------------------------------
FROM docker.io/library/maven:3.9.9-eclipse-temurin-17-alpine AS build

WORKDIR /workspace

# Resolve dependencies before copying sources so Docker can reuse this layer.
COPY pom.xml mvnw ./
COPY .mvn ./.mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Build the deployable Spring Boot jar.
COPY src ./src
RUN ./mvnw -B -q clean package -DskipTests

# ---- Runtime stage -------------------------------------------------------
FROM docker.io/library/eclipse-temurin:17-jre-alpine AS runtime

# Keep only the runtime JRE; no compiler, Maven, or source code remains here.
WORKDIR /app

RUN addgroup -S -g 1001 appgroup \
 && adduser -S -u 1001 -G appgroup appuser

COPY --from=build --chown=appuser:appgroup /workspace/target/team-workspace-*.jar /app/app.jar
RUN chmod 0550 /app/app.jar

USER appuser

EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]