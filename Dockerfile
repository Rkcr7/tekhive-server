FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17.0.1-jdk-slim
COPY --from=build /target/spring-boot-security-postgresql-0.0.1-SNAPSHOT.jar spring-boot-security-postgresql.jar
EXPOSE 8085
ENTRYPOINT [ "java","-jar","spring-boot-security-postgresql.jar" ]