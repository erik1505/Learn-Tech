package com.learntech.backend.service;

import com.learntech.backend.model.EmailVerificationCode;
import com.learntech.backend.model.EmailVerificationPurpose;
import com.learntech.backend.repository.EmailVerificationCodeRepository;
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
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LIFETIME_MINUTES = 10;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeRepository codes;
    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailVerificationService(EmailVerificationCodeRepository codes,
                                    JavaMailSender mailSender,
                                    @Value("${app.mail.from}") String fromAddress) {
        this.codes = codes;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Transactional
    public String enviarCodigo(String login, EmailVerificationPurpose purpose) {
        String normalizedLogin = login.trim().toLowerCase(Locale.ROOT);
        LocalDateTime criadoEm = LocalDateTime.now();
        Optional<EmailVerificationCode> codigoRecente =
                codes.findTopByLoginAndPurposeAndConsumedFalseOrderByCreatedAtDesc(normalizedLogin, purpose);
        if (codigoRecente.isPresent()
                && codigoRecente.get().getCreatedAt().plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(criadoEm)
                && codigoRecente.get().getExpiresAt().isAfter(criadoEm)) {
            return codigoRecente.get().getId();
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
        } catch (MailException erro) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o código. Verifique a configuração SMTP do servidor.",
                    erro
            );
        }

        return challengeId;
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
