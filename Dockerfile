# Build
FROM maven:3-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# Run
FROM eclipse-temurin:26-jre
WORKDIR /app
COPY --from=build /app/target/smarthr-1.0.0.jar app.jar
ENV SPRING_PROFILES_ACTIVE=h2
EXPOSE 8080
ENTRYPOINT ["java", "-Dnet.bytebuddy.experimental=true", "-jar", "app.jar"]
