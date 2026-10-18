package com.learntech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailCodeRequest(
        @NotBlank(message = "A solicitação do código é inválida.")
        @Size(max = 36, message = "A solicitação do código é inválida.")
        String challengeId,

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "[0-9]{6}", message = "O código deve conter seis números.")
        String codigo
) {}
