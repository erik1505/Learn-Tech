package com.learntech.backend.repository;

import com.learntech.backend.model.EmailCodeRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface EmailCodeRateLimitRepository extends JpaRepository<EmailCodeRateLimit, String> {
    void deleteByRequestedAtBefore(LocalDateTime cutoff);
}
