FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app
COPY target/app.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]