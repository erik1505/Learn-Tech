package com.learntech.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class VerificationEmailDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(VerificationEmailDispatcher.class);

    private final VerificationEmailSender sender;

    public VerificationEmailDispatcher(VerificationEmailSender sender) {
        this.sender = sender;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendAfterCommit(VerificationEmailRequest request) {
        try {
            sender.send(request);
        } catch (TaskRejectedException erro) {
            logger.error("Fila de e-mails de verificação cheia; o e-mail não foi enviado.", erro);
        }
    }
}
