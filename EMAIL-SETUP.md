# Configuração de envio de e-mail

O `docker compose` usa SMTP do Gmail por padrão. Para enviar de uma conta Gmail,
configure as credenciais antes de iniciar a aplicação:

1. Copie `.env.example` para `.env`.
2. Substitua `MAIL_USERNAME` pelo endereço Gmail e `MAIL_PASSWORD` por uma senha
   de app do Google (não a senha normal da conta). A senha de app exige que a
   verificação em duas etapas esteja ativada na conta Google.
3. Defina `MAIL_FROM` com o mesmo endereço Gmail autorizado.
4. Inicie ou reinicie a aplicação para carregar a configuração:

   ```sh
   docker compose up --build -d
   ```

Se executar pelo IntelliJ em vez do Docker, o arquivo `.env` não é carregado
automaticamente. Abra **Run > Edit Configurations...**, selecione a configuração
da aplicação Spring Boot e informe as mesmas variáveis no campo **Environment
variables**, por exemplo:

```text
MAIL_USERNAME=seu-endereco@gmail.com;MAIL_PASSWORD=sua-senha-de-app;MAIL_FROM=seu-endereco@gmail.com
```

Sem credenciais válidas, o Gmail recusará a autenticação e a solicitação do
código retornará um erro em vez de indicar que a mensagem foi enviada.

Para desenvolvimento sem entrega real, configure no `.env`:

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

Então inicie também o Maildev, disponível em <http://localhost:1080>:

```sh
docker compose --profile dev up --build -d
```

`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
`MAIL_SMTP_STARTTLS`, `MAIL_SMTP_STARTTLS_REQUIRED` e `MAIL_FROM` são lidos pelo
serviço. O arquivo `.env` não deve ser versionado; não coloque credenciais reais
no código nem no `.env.example`.
