FROM maven:3.9.6-eclipse-temurin-17

WORKDIR /app

# Copia todo o código para o container
COPY . .

# Compila o projeto gerando o .jar
RUN mvn clean package -DskipTests

# Expõe a porta padrão
EXPOSE 8080

# Executa o .jar gerado na pasta target
CMD ["sh", "-c", "java -jar target/*.jar"]
