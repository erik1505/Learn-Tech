package com.learntech.backend.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AuthSession")
public class AuthSession {
    @Id
    @Column(name = "TokenHash", length = 64, nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UserId", nullable = false)
    private Usuario usuario;

    @Column(name = "ExpiresAt", nullable = false)
    private LocalDateTime expiresAt;

    protected AuthSession() {}

    public AuthSession(String tokenHash, Usuario usuario, LocalDateTime expiresAt) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiresAt = expiresAt;
    }

    public String getTokenHash() { return tokenHash; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
