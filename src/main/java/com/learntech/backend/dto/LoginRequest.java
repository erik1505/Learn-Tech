package com.learntech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "E-mail ou telefone é obrigatório")
        @Pattern(
                regexp = "^(?:[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}|[0-9\\s()+-]{10,20})$",
                message = "Informe um e-mail válido ou um telefone com 10 a 11 dígitos."
        )
        String identificador,

        @NotBlank(message = "A senha é obrigatória")
        String senha
) {}