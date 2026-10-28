# Configuração de envio de e-mail com Gmail SMTP

O backend envia códigos de login e de redefinição de senha, além de avisos de
novas atividades, alterações em atividades e notas corrigidas. Os avisos de
atividades e notas são enviados aos e-mails dos alunos com matrícula ativa na
turma. `MAIL_USERNAME` e `MAIL_FROM` identificam a conta que envia as
mensagens. Não é necessário ter um domínio próprio.

## Preparar a conta Gmail

1. Escolha uma conta Gmail para ser o remetente do sistema. É recomendável usar
   uma conta separada da sua conta pessoal.
2. Ative a verificação em duas etapas na Conta Google.
3. Crie uma senha de app em <https://myaccount.google.com/apppasswords>.
   O Google exibe uma senha de 16 caracteres; use-a no sistema, não a senha
   normal da conta.
4. Guarde a senha de app em local seguro. Não a compartilhe nem a envie para o
   repositório. O Google pode revogar senhas de app quando a senha principal da
   conta é alterada.

Senhas de app só ficam disponíveis para algumas contas com verificação em duas
etapas. Contas gerenciadas por organizações, com Proteção Avançada ou com
configurações específicas podem não oferecer essa opção. Consulte a
[documentação do Google](https://support.google.com/accounts/answer/185833).

## Executar pelo IntelliJ

Abra **Run > Edit Configurations...**, selecione a configuração da aplicação
Spring Boot e preencha **Environment variables**. Substitua o e-mail e a senha
de exemplo pelos valores da sua conta:

```text
MAIL_HOST=smtp.gmail.com;MAIL_PORT=587;MAIL_USERNAME=seu-email@gmail.com;MAIL_PASSWORD=sua-senha-de-app;MAIL_SMTP_AUTH=true;MAIL_SMTP_STARTTLS=true;MAIL_SMTP_STARTTLS_REQUIRED=true;MAIL_FROM=seu-email@gmail.com
```

Use o mesmo endereço Gmail em `MAIL_USERNAME` e `MAIL_FROM`. Salve e reinicie
a aplicação para carregar as variáveis.

## Executar pelo Docker Compose

1. Copie `.env.example` para `.env`.
2. Edite `.env` e informe seu endereço Gmail em `MAIL_USERNAME` e `MAIL_FROM`,
   além da senha de app em `MAIL_PASSWORD`.
3. Na pasta que contém `compose.yaml`, inicie o backend:

   ```sh
   docker compose up --build -d app
   ```

O Docker Compose carrega o `.env` automaticamente. Confira se o serviço iniciou
com `docker compose ps`. Não versione o `.env`; ele contém uma credencial
secreta. O `.env.example` contém somente valores ilustrativos.

O Gmail aplica limites de envio e pode bloquear temporariamente mensagens se
detectar atividade incomum. Para mais informações, consulte os
[limites de envio do Gmail](https://support.google.com/a/answer/166852).
Uma conta Gmail pessoal pode servir para desenvolvimento ou baixo volume, mas
não é um serviço de envio transacional com garantia de entrega.

## Teste local sem entrega real

Para testar sem enviar e-mails a caixas reais, você também pode usar Maildev
com Docker:

```sh
docker compose --profile dev up --build -d
```

Configure a aplicação com `MAIL_HOST=localhost` se ela roda pelo IntelliJ fora
do Docker (use `maildev` quando o próprio backend roda no Compose), `MAIL_PORT=1025`,
autenticação desativada e STARTTLS desativado. Consulte as mensagens capturadas
em <http://localhost:1080>.

## Variáveis SMTP

`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
`MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_STARTTLS_REQUIRED` e
`MAIL_FROM` são lidos pelo backend. Para Gmail, use `smtp.gmail.com`, porta
`587`, autenticação e STARTTLS ativados.

## SMS sem provedor pago

Para enviar SMS sem tarifa de API, use o projeto open-source
[SMS Gateway para Android](https://github.com/capcom6/android-sms-gateway) no
modo **Local Server**. Instale o aplicativo em um Android com chip, inicie o
servidor local e configure no `.env` o endereço, usuário e senha exibidos pelo
aplicativo:

```dotenv
SMS_GATEWAY_URL=http://192.168.1.100:8080/message
SMS_GATEWAY_USERNAME=usuario-exibido-no-app
SMS_GATEWAY_PASSWORD=senha-exibida-no-app
```

O backend deve conseguir acessar o IP do telefone pela rede local ou por uma
VPN. Não exponha a porta do telefone diretamente à internet. Em Docker, o host
do backend precisa ter rota até o Android. No login, a opção de SMS só aparece
quando o gateway está configurado e a conta possui telefone válido com DDD. Na
redefinição de senha, o usuário escolhe o canal; a entrega por SMS depende das
mesmas configurações e de um telefone válido cadastrado.

O aplicativo e a API local não cobram tarifa de provedor, mas cada mensagem usa
o plano/SIM do telefone; só será gratuito se a operadora incluir SMS no plano.
