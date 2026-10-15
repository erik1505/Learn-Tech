# Etapa de Build com Maven instalado
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copia o pom.xml e baixa as dependências
COPY pom.xml ./
RUN mvn dependency:go-offline

# Copia o código fonte e faz o empacotamento
COPY src/ src/
RUN mvn clean package -DskipTests

# Etapa final para rodar a aplicação
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
