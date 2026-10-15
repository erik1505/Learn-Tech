FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

# Roda o maven sem suprimir nada e mostrando erros de compilação diretamente
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
