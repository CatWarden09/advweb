# separate layers allow not to download all dependencies if only the code has changed (src folder)
# mvnw is used because it allows not to pre-install Maven to the machine from where the project is built
FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

RUN mkdir -p /app/uploads

COPY pom.xml mvnw ./
COPY .mvn/ .mvn/

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package


EXPOSE 8080

ENTRYPOINT ["java","-jar","target/advweb-0.3.jar"]