FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app
COPY pom.xml .
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package

COPY target/advweb-0.3.jar app.jar

# Экспонируем порт
EXPOSE 8080

# Команда запуска
ENTRYPOINT ["java","-jar","app.jar"]