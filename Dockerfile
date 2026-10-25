# Stage 1: Build React Frontend
FROM node:22-alpine AS frontend-build
WORKDIR /app
COPY frontend/package*.json ./
RUN npm install --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend with embedded Frontend
FROM maven:3.9-eclipse-temurin-21 AS backend-build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -B -q dependency:go-offline
COPY backend/src ./src
# Embed built frontend static assets into Spring Boot's classpath
COPY --from=frontend-build /app/dist ./src/main/resources/static
RUN mvn -B package -DskipTests

# Stage 3: Lightweight Runtime Container
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend-build /app/target/baaki-backend-0.1.0.jar app.jar
ENV TZ=Asia/Kolkata
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
