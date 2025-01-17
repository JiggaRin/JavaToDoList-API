FROM eclipse-temurin:23-jdk

WORKDIR /app
COPY target/todo-list-0.0.1.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
