FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia todo o projeto para dentro do container para garantir que nada fique de fora
COPY . .

# Roda o maven exibindo mais detalhes do erro caso ocorra falha
RUN mvn clean package -DskipTests -X

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
