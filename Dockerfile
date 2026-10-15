FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY . .

# Compila pulando testes e erros de documentação/checkstyle
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia o arquivo .jar gerado na pasta target
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
