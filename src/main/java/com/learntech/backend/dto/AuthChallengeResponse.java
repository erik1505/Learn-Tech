package com.learntech.backend.dto;

public record AuthChallengeResponse(String challengeId, String message, String testCode) {
    public AuthChallengeResponse(String challengeId, String message) {
        this(challengeId, message, null);
    }
}
