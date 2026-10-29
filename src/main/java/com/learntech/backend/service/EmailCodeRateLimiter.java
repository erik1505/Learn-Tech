package com.learntech.backend.service;

import com.learntech.backend.model.EmailCodeRateLimit;
import com.learntech.backend.repository.EmailCodeRateLimitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class EmailCodeRateLimiter {
    private static final int COOLDOWN_SECONDS = 60;

    private final EmailCodeRateLimitRepository repository;

    public EmailCodeRateLimiter(EmailCodeRateLimitRepository repository) {
        this.repository = repository;
    }

    public synchronized void reserve(String email, String remoteAddress) {
        LocalDateTime now = LocalDateTime.now();
        List<String> keys = new ArrayList<>();
        keys.add(key("EMAIL", email.trim().toLowerCase(Locale.ROOT)));
        if (remoteAddress != null && !remoteAddress.isBlank()) {
            keys.add(key("IP", remoteAddress.trim()));
        }

        long waitSeconds = 0;
        for (String id : keys) {
            var previous = repository.findById(id);
            if (previous.isPresent()) {
                LocalDateTime allowedAt = previous.get().getRequestedAt().plusSeconds(COOLDOWN_SECONDS);
                if (allowedAt.isAfter(now)) {
                    waitSeconds = Math.max(waitSeconds, Duration.between(now, allowedAt).getSeconds() + 1);
                }
            }
        }
        if (waitSeconds > 0) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Um novo código só pode ser solicitado após " + waitSeconds + " segundos.");
        }

        List<EmailCodeRateLimit> limits = keys.stream()
                .map(id -> repository.findById(id)
                        .orElseGet(() -> new EmailCodeRateLimit(id, now)))
                .peek(limit -> limit.setRequestedAt(now))
                .toList();
        repository.saveAll(limits);
    }

    @Scheduled(fixedDelayString = "${app.verification.rate-limit-cleanup-ms:3600000}")
    @Transactional
    public void removerLimitesExpirados() {
        repository.deleteByRequestedAtBefore(LocalDateTime.now().minusHours(24));
    }

    private String key(String type, String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return type + ":" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível para proteger o limite de envio.", erro);
        }
    }
}
