package com.learntech.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "EmailCodeRateLimit")
public class EmailCodeRateLimit {
    @Id
    @Column(name = "Id", length = 70, nullable = false)
    private String id;

    @Column(name = "RequestedAt", nullable = false)
    private LocalDateTime requestedAt;

    protected EmailCodeRateLimit() {}

    public EmailCodeRateLimit(String id, LocalDateTime requestedAt) {
        this.id = id;
        this.requestedAt = requestedAt;
    }

    public String getId() { return id; }
    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }
}
