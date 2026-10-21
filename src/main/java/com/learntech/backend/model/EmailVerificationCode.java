package com.learntech.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "EmailVerificationCode")
public class EmailVerificationCode {
    @Id
    @Column(name = "Id", length = 36, nullable = false)
    private String id;

    @Column(name = "Login", length = 100, nullable = false)
    private String login;

    @Enumerated(EnumType.STRING)
    @Column(name = "Purpose", length = 20, nullable = false)
    private EmailVerificationPurpose purpose;

    @Column(name = "CodeHash", length = 64, nullable = false)
    private String codeHash;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "CreatedAt", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "Attempts", nullable = false)
    private int attempts;

    @Column(name = "Consumed", nullable = false)
    private boolean consumed;

    protected EmailVerificationCode() {}

    public EmailVerificationCode(String id, String login, EmailVerificationPurpose purpose,
                                 String codeHash, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.id = id;
        this.login = login;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public String getLogin() { return login; }
    public EmailVerificationPurpose getPurpose() { return purpose; }
    public String getCodeHash() { return codeHash; }
    public void setCodeHash(String codeHash) { this.codeHash = codeHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public boolean isConsumed() { return consumed; }
    public void setConsumed(boolean consumed) { this.consumed = consumed; }
}
