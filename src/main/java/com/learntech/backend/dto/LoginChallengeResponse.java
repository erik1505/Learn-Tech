package com.learntech.backend.dto;

public record LoginChallengeResponse(
        String challengeId,
        String emailMascarado,
        String telefoneMascarado,
        boolean smsDisponivel,
        String message
) {}