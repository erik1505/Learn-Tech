# Configuração de envio de e-mail com Resend

O backend envia os códigos de login e de redefinição de senha por SMTP. O
destinatário é o endereço informado/cadastrado por cada usuário; as variáveis
abaixo configuram somente o serviço e o endereço remetente.

## Preparar o Resend

1. Crie uma conta em <https://resend.com/>.
2. No painel, adicione e verifique um domínio remetente seguindo as instruções
   de DNS do Resend. Use um endereço desse domínio em `MAIL_FROM`.
3. Crie uma API key no painel. Trate-a como senha: não a compartilhe nem a
   envie para o repositório.
4. Confira os limites e condições atuais do plano gratuito em
   <https://resend.com/pricing>.

O Resend exige uma API key e um domínio verificado para envio SMTP. O endereço
em `MAIL_FROM` precisa pertencer a um domínio verificado na conta.

## Executar pelo IntelliJ

Na configuração **Run > Edit Configurations...** da aplicação Spring Boot,
preencha **Environment variables** com os valores da sua conta:

```text
MAIL_HOST=smtp.resend.com;MAIL_PORT=587;MAIL_USERNAME=resend;MAIL_PASSWORD=re_sua_api_key;MAIL_SMTP_AUTH=true;MAIL_SMTP_STARTTLS=true;MAIL_SMTP_STARTTLS_REQUIRED=true;MAIL_FROM=no-reply@seu-dominio-verificado.com
```

Troque `re_sua_api_key` pela chave criada no Resend e `MAIL_FROM` por um
remetente do domínio verificado. Salve e reinicie a aplicação para carregar as
variáveis.

## Executar pelo Docker Compose

1. Copie `.env.example` para `.env`.
2. Edite `.env`: substitua `MAIL_PASSWORD` pela API key real e `MAIL_FROM` por
   um remetente do seu domínio verificado.
3. Na pasta que contém `compose.yaml`, inicie o backend:

   ```sh
   docker compose up --build -d app
   ```

O Docker Compose carrega o `.env` automaticamente. Para conferir o estado do
serviço:

```sh
docker compose ps
```

Não versione o `.env`; ele contém uma credencial secreta. O `.env.example`
contém apenas valores ilustrativos.

## Teste local sem entrega real

Para desenvolvimento, também é possível usar o Maildev em vez do Resend. Essa
opção precisa do Docker em execução:

```sh
docker compose --profile dev up --build -d
```

Configure o backend para usar `MAIL_HOST=maildev`, porta `1025`, autenticação
desativada e STARTTLS desativado. Se o backend roda pelo IntelliJ fora do
Docker, `maildev` não será resolvido como host: use `MAIL_HOST=localhost`.
Consulte as mensagens capturadas em <http://localhost:1080>. Essas mensagens
não são entregues a caixas de e-mail reais.

## Variáveis SMTP

`MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`,
`MAIL_SMTP_AUTH`, `MAIL_SMTP_STARTTLS`, `MAIL_SMTP_STARTTLS_REQUIRED` e
`MAIL_FROM` são lidos pelo backend. Para o Resend, use `smtp.resend.com`,
usuário `resend`, a API key como senha e STARTTLS na porta `587`.

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
do backend precisa ter rota até o Android. A opção de SMS só aparece quando o
gateway está configurado e a conta possui telefone válido com DDD.

O aplicativo e a API local não cobram tarifa de provedor, mas cada mensagem usa
o plano/SIM do telefone; só será gratuito se a operadora incluir SMS no plano.
