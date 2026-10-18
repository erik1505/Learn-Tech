package com.learntech.backend.repository;

import com.learntech.backend.model.EmailVerificationCode;
import com.learntech.backend.model.EmailVerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, String> {
    void deleteByLoginAndPurposeAndConsumedFalse(String login, EmailVerificationPurpose purpose);
    Optional<EmailVerificationCode> findTopByLoginAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String login, EmailVerificationPurpose purpose);
}
