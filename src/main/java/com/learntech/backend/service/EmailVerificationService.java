package com.learntech.backend.service;

import com.learntech.backend.dto.AuthChallengeResponse;
import com.learntech.backend.model.EmailVerificationCode;
import com.learntech.backend.model.EmailVerificationPurpose;
import com.learntech.backend.repository.EmailVerificationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
    private static final Logger logger = LoggerFactory.getLogger(EmailVerificationService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LIFETIME_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeRepository codes;
    private final JavaMailSender mailSender;
    private final LocalSmsGatewayService smsGateway;
    private final String fromAddress;
    private final String mailHost;

    public EmailVerificationService(EmailVerificationCodeRepository codes,
                                    JavaMailSender mailSender,
                                    LocalSmsGatewayService smsGateway,
                                    @Value("${app.mail.from}") String fromAddress,
                                    @Value("${spring.mail.host:localhost}") String mailHost) {
        this.codes = codes;
        this.mailSender = mailSender;
        this.smsGateway = smsGateway;
        this.fromAddress = fromAddress;
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
    public AuthChallengeResponse enviarCodigoLogin(String challengeId, String canal, String telefone) {
        EmailVerificationCode desafio = desafioLoginAtivo(challengeId);
        LocalDateTime agora = LocalDateTime.now();
        if (!"PENDING".equals(desafio.getCodeHash())
                && desafio.getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(agora)) {
            return new AuthChallengeResponse(
                    challengeId,
                    "Código enviado recentemente. Verifique sua caixa de entrada."
            );
        }
        String codigo = "%06d".formatted(RANDOM.nextInt(1_000_000));
        String mensagem = "Seu código de acesso Learn Tech é " + codigo
                + ". Ele expira em " + CODE_LIFETIME_MINUTES + " minutos.";

        if ("EMAIL".equals(canal)) {
            SimpleMailMessage email = new SimpleMailMessage();
            email.setFrom(fromAddress);
            email.setTo(desafio.getLogin());
            email.setSubject("Código de acesso - Learn Tech");
            email.setText(mensagem + " Se você não solicitou este código, ignore esta mensagem.");
            try {
                mailSender.send(email);
            } catch (MailException erro) {
                logger.error("Falha ao enviar código de login pelo SMTP {}.", mailHost, erro);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Não foi possível enviar o código. Verifique a configuração SMTP do servidor.", erro);
            }
        } else if ("SMS".equals(canal)) {
            smsGateway.enviarCodigo(telefone, mensagem);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canal de envio inválido.");
        }

        desafio.setCodeHash(hash(challengeId, codigo));
        desafio.setCreatedAt(agora);
        desafio.setExpiresAt(agora.plusMinutes(CODE_LIFETIME_MINUTES));
        desafio.setAttempts(0);
        codes.save(desafio);

        String destino = "SMS".equals(canal) ? "SMS" : "e-mail";
        String message = "maildev".equalsIgnoreCase(mailHost.trim()) && "EMAIL".equals(canal)
                ? "Código enviado ao e-mail de teste. Consulte http://localhost:1080."
                : "Código enviado por " + destino + ".";
        return new AuthChallengeResponse(challengeId, message);
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

    @Transactional
    public AuthChallengeResponse enviarCodigo(String login, EmailVerificationPurpose purpose) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        LocalDateTime criadoEm = LocalDateTime.now();
        Optional<EmailVerificationCode> codigoRecente =
                codes.findTopByLoginAndPurposeAndConsumedFalseOrderByCreatedAtDesc(normalizedLogin, purpose);
        if (codigoRecente.isPresent()
                && codigoRecente.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(criadoEm)
                && codigoRecente.get().getExpiresAt().isAfter(criadoEm)) {
            return new AuthChallengeResponse(codigoRecente.get().getId(), "Código já enviado recentemente. Verifique sua caixa de entrada.");
        }

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

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(fromAddress);
        mensagem.setTo(normalizedLogin);
        mensagem.setSubject(purpose == EmailVerificationPurpose.LOGIN
                ? "Código de acesso - Learn Tech"
                : "Código para redefinição de senha - Learn Tech");
        mensagem.setText("Seu código de verificação é " + codigo
                + ". Ele expira em " + CODE_LIFETIME_MINUTES
                + " minutos e só pode ser usado uma vez. Se você não solicitou este código, ignore esta mensagem.");

        try {
            mailSender.send(mensagem);
            return new AuthChallengeResponse(
                    challengeId,
                    "maildev".equalsIgnoreCase(mailHost.trim())
                            ? "Código enviado ao e-mail de teste. Consulte http://localhost:1080."
                            : purpose == EmailVerificationPurpose.LOGIN
                                    ? "Código enviado para o e-mail cadastrado."
                                    : "Se o e-mail estiver cadastrado, você receberá um código para redefinir a senha."
            );
        } catch (MailException erro) {
            logger.error("Falha ao enviar código de verificação pelo servidor SMTP {}.", mailHost, erro);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o código. Verifique a configuração SMTP do servidor.",
                    erro
            );
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
