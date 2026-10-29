package com.learntech.backend.service;

import com.learntech.backend.dto.AuthChallengeResponse;
import com.learntech.backend.model.EmailVerificationCode;
import com.learntech.backend.model.EmailVerificationPurpose;
import com.learntech.backend.repository.EmailVerificationCodeRepository;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LIFETIME_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeRepository codes;
    private final EmailCodeRateLimiter rateLimiter;
    private final ApplicationEventPublisher events;
    private final LocalSmsGatewayService smsGateway;
    private final String mailHost;

    public EmailVerificationService(EmailVerificationCodeRepository codes,
                                    EmailCodeRateLimiter rateLimiter,
                                    ApplicationEventPublisher events,
                                    LocalSmsGatewayService smsGateway,
                                    @Value("${spring.mail.host:localhost}") String mailHost) {
        this.codes = codes;
        this.rateLimiter = rateLimiter;
        this.events = events;
        this.smsGateway = smsGateway;
        this.mailHost = mailHost;
    }

    @Transactional
    public String prepararDesafioLogin(String login) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        LocalDateTime agora = LocalDateTime.now();
        Optional<EmailVerificationCode> recente = codes
                .findTopByLoginAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
                        normalizedLogin, EmailVerificationPurpose.LOGIN);
        if (recente.isPresent() && recente.get().getExpiresAt().isAfter(agora)) {
            return recente.get().getId();
        }

        String challengeId = UUID.randomUUID().toString();
        codes.deleteByLoginAndPurposeAndConsumedFalse(normalizedLogin, EmailVerificationPurpose.LOGIN);
        codes.save(new EmailVerificationCode(
                challengeId,
                normalizedLogin,
                EmailVerificationPurpose.LOGIN,
                "PENDING",
                agora,
                agora.plusMinutes(5)
        ));
        return challengeId;
    }

    @Transactional(readOnly = true)
    public String loginDoDesafioLogin(String challengeId) {
        return desafioLoginAtivo(challengeId).getLogin();
    }

    @Transactional
    public AuthChallengeResponse enviarCodigoLoginReservado(
            String challengeId,
            String canal,
            String telefone,
            String remoteAddress) {
        EmailVerificationCode desafio = desafioLoginAtivo(challengeId);
        LocalDateTime agora = LocalDateTime.now();
        boolean testeLocal = "SMS".equals(canal)
                && smsGateway.modoTesteLocal(remoteAddress);
        String codigo = "%06d".formatted(RANDOM.nextInt(1_000_000));
        String mensagem = "Seu código de acesso Learn Tech é " + codigo
                + ". Ele expira em " + CODE_LIFETIME_MINUTES + " minutos.";

        boolean codigoDeTeste = false;
        if ("EMAIL".equals(canal)) {
            events.publishEvent(new VerificationEmailRequest(
                    desafio.getLogin(),
                    "Código de acesso - Learn Tech",
                    mensagem + " Se você não solicitou este código, ignore esta mensagem."
            ));
        } else if ("SMS".equals(canal)) {
            codigoDeTeste = smsGateway.enviarCodigo(telefone, mensagem, remoteAddress);
        }

        desafio.setCodeHash(hash(challengeId, codigo));
        desafio.setCreatedAt(agora);
        desafio.setExpiresAt(agora.plusMinutes(CODE_LIFETIME_MINUTES));
        desafio.setAttempts(0);
        codes.save(desafio);

        String message = codigoDeTeste
                ? "Modo de teste local: nenhum SMS foi enviado."
                : "EMAIL".equals(canal)
                        ? mensagemEnvio(EmailVerificationPurpose.LOGIN, canal)
                        : "Código enviado por SMS.";
        return new AuthChallengeResponse(challengeId, message, codigoDeTeste ? codigo : null);
    }

    private EmailVerificationCode desafioLoginAtivo(String challengeId) {
        if (challengeId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Desafio inválido ou expirado.");
        }
        return codes.findById(challengeId)
                .filter(codigo -> codigo.getPurpose() == EmailVerificationPurpose.LOGIN
                        && !codigo.isConsumed()
                        && codigo.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Desafio inválido ou expirado."));
    }

    public void reservarSolicitacao(String login, String remoteAddress) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        validarEmail(normalizedLogin);
        rateLimiter.reserve(normalizedLogin, remoteAddress);
    }

    @Transactional
    public AuthChallengeResponse enviarCodigoReservado(
            String login,
            EmailVerificationPurpose purpose,
            String canal,
            String telefone,
            String remoteAddress) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        validarCanal(canal);
        if ("EMAIL".equals(canal)) {
            validarEmail(normalizedLogin);
        }
        LocalDateTime criadoEm = LocalDateTime.now();
        boolean testeLocal = "SMS".equals(canal)
                && smsGateway.modoTesteLocal(remoteAddress);

        String challengeId = UUID.randomUUID().toString();
        String codigo = "%06d".formatted(RANDOM.nextInt(1_000_000));

        codes.deleteByLoginAndPurposeAndConsumedFalse(normalizedLogin, purpose);
        codes.save(new EmailVerificationCode(
                challengeId,
                normalizedLogin,
                purpose,
                hash(challengeId, codigo),
                criadoEm,
                criadoEm.plusMinutes(CODE_LIFETIME_MINUTES)
        ));

        String mensagemTexto = "Seu código de verificação é " + codigo
                + ". Ele expira em " + CODE_LIFETIME_MINUTES
                + " minutos e só pode ser usado uma vez. Se você não solicitou este código, ignore esta mensagem.";
        boolean codigoDeTeste = false;
        if ("SMS".equals(canal)) {
            codigoDeTeste = smsGateway.enviarCodigo(telefone, mensagemTexto, remoteAddress);
        } else if ("EMAIL".equals(canal)) {
            events.publishEvent(new VerificationEmailRequest(
                    normalizedLogin,
                    purpose == EmailVerificationPurpose.LOGIN
                            ? "Código de acesso - Learn Tech"
                            : "Código para redefinição de senha - Learn Tech",
                    mensagemTexto
            ));
        }

        String mensagem = codigoDeTeste
                ? "Modo de teste local: nenhum SMS foi enviado."
                : mensagemEnvio(purpose, canal);
        return new AuthChallengeResponse(challengeId, mensagem, codigoDeTeste ? codigo : null);
    }

    private String mensagemEnvio(EmailVerificationPurpose purpose, String canal) {
        if (purpose == EmailVerificationPurpose.PASSWORD_RESET) {
            return "Se os dados estiverem cadastrados, você receberá um código pelo canal escolhido.";
        }
        if ("SMS".equals(canal)) {
            return "Código enviado por SMS.";
        }
        return "maildev".equalsIgnoreCase(mailHost.trim())
                ? "Solicitação recebida. Consulte o e-mail de teste em http://localhost:1080."
                : "Solicitação recebida. Confira o e-mail em instantes.";
    }

    private void validarCanal(String canal) {
        if (!"EMAIL".equals(canal) && !"SMS".equals(canal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canal de envio inválido.");
        }
    }

    private void validarEmail(String email) {
        try {
            InternetAddress address = new InternetAddress(email, true);
            address.validate();
            if (!email.equals(address.getAddress()) || address.getPersonal() != null) {
                throw new AddressException("O endereço deve conter apenas um e-mail.");
            }
        } catch (AddressException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail de destino inválido.", erro);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<String> verificarCodigo(String challengeId, String codigo, EmailVerificationPurpose purpose) {
        if (challengeId == null || codigo == null) {
            return Optional.empty();
        }

        Optional<EmailVerificationCode> encontrado = codes.findById(challengeId);
        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        EmailVerificationCode desafio = encontrado.get();
        if (desafio.getPurpose() != purpose
                || desafio.isConsumed()
            || "PENDING".equals(desafio.getCodeHash())
                || desafio.getAttempts() >= MAX_ATTEMPTS
                || desafio.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        boolean codigoCorreto = MessageDigest.isEqual(
                hash(challengeId, codigo).getBytes(StandardCharsets.US_ASCII),
                desafio.getCodeHash().getBytes(StandardCharsets.US_ASCII)
        );
        if (!codigoCorreto) {
            desafio.setAttempts(desafio.getAttempts() + 1);
            codes.save(desafio);
            return Optional.empty();
        }

        desafio.setConsumed(true);
        codes.save(desafio);
        return Optional.of(desafio.getLogin());
    }

    private String hash(String challengeId, String codigo) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((challengeId + ":" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível para proteger o código de verificação.", erro);
        }
    }
}
