# ─────────────────────────────────────────
# Stage 1: Build
# ─────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build
COPY pom.xml .
# Download dependencies first (cached layer)
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn clean package -DskipTests -q

# ─────────────────────────────────────────
# Stage 2: Runtime
# ─────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="BlastRadius"
LABEL description="Engineering Dependency Intelligence Platform"

RUN addgroup -S blastradius && adduser -S blastradius -G blastradius
WORKDIR /app

COPY --from=builder /build/target/blastradius-*.jar app.jar

RUN chown blastradius:blastradius app.jar
USER blastradius

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
