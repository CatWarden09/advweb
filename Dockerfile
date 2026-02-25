FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

RUN mkdir -p /app/uploads

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests


EXPOSE 8080

ENTRYPOINT ["java","-jar","target/advweb-0.3.jar"]