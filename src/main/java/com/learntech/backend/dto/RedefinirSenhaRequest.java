package com.learntech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank(message = "A solicitação do código é inválida.")
        @Size(max = 36, message = "A solicitação do código é inválida.")
        String challengeId,

        @NotBlank(message = "O código é obrigatório.")
        @Pattern(regexp = "[0-9]{6}", message = "O código deve conter seis números.")
        String codigo,

        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 6, message = "A senha precisa ter pelo menos 6 caracteres.")
        String novaSenha
) {}