FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

# Adicionamos o -X para o Maven mostrar o motivo exato da falha
RUN mvn clean package -DskipTests -X

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
