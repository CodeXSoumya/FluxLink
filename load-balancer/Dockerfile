FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY load-balancer/pom.xml load-balancer/
COPY url-shortener/pom.xml url-shortener/
# Resolve dependencies to cache them
RUN mvn -B dependency:go-offline
COPY load-balancer/src ./load-balancer/src

RUN mvn clean package -pl load-balancer -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/load-balancer/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]
