FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app
ENV MAVEN_OPTS="-Xms128m -Xmx512m"

COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
COPY index.html ./src/main/resources/static/index.html
COPY Style ./src/main/resources/static/Style
COPY paginas ./src/main/resources/static/paginas
COPY src/image.png ./src/main/resources/static/src/image.png
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=50.0 -XX:ActiveProcessorCount=1 -XX:+UseSerialGC"

COPY --from=build /app/target/learn-tech-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
