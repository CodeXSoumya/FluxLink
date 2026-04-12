FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY load-balancer/pom.xml load-balancer/
COPY url-shortener/pom.xml url-shortener/
# Resolve dependencies to cache them
RUN mvn -B dependency:go-offline || true
COPY url-shortener/src ./url-shortener/src

RUN mvn clean package -pl url-shortener -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/url-shortener/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
