# Build and run the unit tests with Java 21 (no local JDK or Maven needed)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre
ENV TZ=Asia/Kolkata
WORKDIR /app
COPY --from=build /app/target/baaki-backend-0.1.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
