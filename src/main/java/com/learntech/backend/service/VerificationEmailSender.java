package com.learntech.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class VerificationEmailSender {
    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailSender.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String mailHost;

    public VerificationEmailSender(JavaMailSender mailSender,
                                   @Value("${app.mail.from}") String fromAddress,
                                   @Value("${spring.mail.host:localhost}") String mailHost) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailHost = mailHost;
    }

    @Async("verificationMailExecutor")
    public void send(VerificationEmailRequest request) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromAddress);
        email.setTo(request.recipient());
        email.setSubject(request.subject());
        email.setText(request.body());
        try {
            mailSender.send(email);
        } catch (MailException erro) {
            logger.error("Falha ao enviar código pelo SMTP {}. Nenhuma nova tentativa será feita automaticamente.",
                    mailHost, erro);
        }
    }
}
