# ── Stage 1: Build ───────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom files first for dependency caching
COPY pom.xml ./
COPY helpers/pom.xml helpers/
COPY models/pom.xml models/
COPY user/pom.xml user/
COPY auth/pom.xml auth/
COPY college/pom.xml college/
COPY company/pom.xml company/
COPY student/pom.xml student/
COPY drive/pom.xml drive/
COPY app/pom.xml app/

# Download dependencies (cached unless pom.xml changes)
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copy source code
COPY . .

# Build the fat JAR
RUN mvn clean package -DskipTests -pl app -am -B

# ── Stage 2: Run (slim JRE, not full JDK) ───────────────────
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /app/app/target/app-1.0.0.jar app.jar

# Railway sets PORT automatically
ENV PORT=8080
EXPOSE ${PORT}

# JVM tuned for Railway free/starter tier (512MB RAM)
CMD ["java", \
     "-Xmx384m", \
     "-Xms128m", \
     "-XX:+UseG1GC", \
     "-XX:+UseStringDeduplication", \
     "-jar", "app.jar"]