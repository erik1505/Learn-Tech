# Configuração de envio de e-mail

O `docker compose` usa o Maildev por padrão. Ele captura as mensagens para teste,
mas não as entrega a caixas de e-mail reais. As mensagens locais ficam disponíveis
em <http://localhost:1080>.

Para enviar códigos de login e recuperação de senha para endereços reais:

1. Copie `.env.example` para `.env`.
2. Preencha as variáveis com os dados do seu provedor SMTP. O exemplo usa Gmail:
   use uma senha de app, não a senha normal da conta, e configure `MAIL_FROM`
   com um endereço autorizado por esse provedor.
3. Reinicie a aplicação para carregar a configuração:

   ```sh
   docker compose up --build -d
   ```

`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`,
`MAIL_SMTP_STARTTLS`, `MAIL_SMTP_STARTTLS_REQUIRED` e `MAIL_FROM` são lidos pelo
serviço. O arquivo `.env` não deve ser versionado; não coloque credenciais reais
no código nem no `.env.example`.
