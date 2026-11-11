# Learn Tech

Sistema web com frontend estático e backend Java/Spring Boot. O backend também
serve as páginas do frontend; não é necessário iniciar um servidor frontend
separado.

## Requisitos

- Java 21 para executar localmente.
- Maven 3.9.x para compilar e executar localmente. A imagem de build do Docker
  usa Maven 3.9.9.
- Docker Engine e Docker Compose v2, caso escolha a execução em contêiner.
- Navegador atualizado. Não é necessário instalar Node.js ou npm.

## Tecnologias e versões

| Tecnologia | Versão ou observação |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.6 |
| Maven | 3.9.9 na imagem Docker; o projeto local usa Maven instalado no sistema |
| SQLite JDBC | 3.49.1.0 |
| HTML, CSS e JavaScript | Frontend sem framework; não há versões de linguagem declaradas no projeto |
| Docker runtime | Eclipse Temurin 21 JRE, imagem Jammy |
| Maildev | `latest` no Compose, sem versão fixada; opcional para testes de e-mail |

As versões transitivas do Spring, Hibernate e demais dependências são
gerenciadas pelo Spring Boot 3.5.6 no [pom.xml](pom.xml).

## Arquivos necessários

Para compilar e executar fora do Docker, mantenha disponíveis:

- `pom.xml` e `src/`, com o código Java e as configurações em
  `src/main/resources/`.
- `index.html`, `paginas/`, `Style/` e `src/image.png`, que formam o frontend
  empacotado pelo Maven junto com o backend.
- O diretório `data/`, usado para armazenar o banco SQLite local. Ele é criado
  abaixo se ainda não existir.

Para executar com Docker, também são necessários `Dockerfile` e `compose.yaml`.
O arquivo `.env` é opcional e serve para configurar serviços externos; use
`.env.example` como referência. Não compartilhe nem versione credenciais reais.

`modelos-csv/` e `anotacoes/` contêm materiais de apoio. Não são necessários
para a execução normal da aplicação. O diretório `target/` é gerado pelo Maven
e não precisa ser mantido para compilar.

## Execução local

Na raiz do projeto, confirme as ferramentas:

```sh
java -version
mvn -version
```

Crie o diretório de dados (se necessário) e inicie a aplicação:

```sh
mkdir -p data
mvn spring-boot:run
```

Acesse <http://localhost:8080>. O banco é criado/atualizado pelo Hibernate em
`data/learntech.db` por padrão. Para manter os dados, não remova esse arquivo.

Também é possível gerar e executar o JAR:

```sh
mvn clean package
java -jar target/learn-tech-backend-0.0.1-SNAPSHOT.jar
```

## Execução com Docker Compose

Na raiz do projeto, crie o arquivo de ambiente e configure as credenciais de
e-mail se precisar enviar mensagens reais:

```sh
cp .env.example .env
```

Edite `.env`: para Gmail, use uma senha de app em `MAIL_PASSWORD`, não a senha
normal da conta. Os valores de SMS devem ser preenchidos apenas se houver um
gateway Android configurado. Mais detalhes estão em
[anotacoes/EMAIL-SETUP.md](anotacoes/EMAIL-SETUP.md).

Construa e inicie o sistema:

```sh
docker compose up --build -d
```

A aplicação estará em <http://localhost:8080>. O SQLite fica no volume Docker
`sqlite_data`, preservado ao parar os contêineres com `docker compose down`.
Para acompanhar o serviço e os logs:

```sh
docker compose ps
docker compose logs -f app
```

## E-mail de teste com Maildev

O Maildev captura mensagens sem entregá-las a caixas reais. Antes de iniciar,
configure em `.env`:

```dotenv
MAIL_HOST=maildev
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_SMTP_AUTH=false
MAIL_SMTP_STARTTLS=false
MAIL_SMTP_STARTTLS_REQUIRED=false
MAIL_FROM=no-reply@learntech.local
```

Inicie os serviços e abra a caixa de entrada de teste:

```sh
docker compose --profile dev up --build -d
```

O Maildev fica em <http://localhost:1080>. Para executar o backend diretamente
na máquina e o Maildev no Docker, use `MAIL_HOST=localhost` em vez de
`MAIL_HOST=maildev`.

## Configurações opcionais

- `DB_URL`: URL JDBC do SQLite. O padrão local é
  `jdbc:sqlite:./data/learntech.db`; no Compose, os dados ficam em
  `/data/learntech.db` dentro do volume persistente.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
  `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_STARTTLS_REQUIRED` e `MAIL_FROM`: envio de
  e-mail. Sem credenciais SMTP válidas, os recursos que enviam e-mail não
  conseguem entregar mensagens.
- `SMS_GATEWAY_URL`, `SMS_GATEWAY_USERNAME` e `SMS_GATEWAY_PASSWORD`: envio por
  um gateway Android acessível pelo backend.

As configurações podem ser fornecidas como variáveis de ambiente ao executar
localmente ou definidas em `.env` para o Docker Compose. O arquivo `.env` pode
conter segredos e não deve ser enviado ao repositório.