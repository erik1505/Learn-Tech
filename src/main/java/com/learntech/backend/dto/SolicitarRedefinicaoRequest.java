package com.learntech.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SolicitarRedefinicaoRequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String email,

        @Pattern(regexp = "EMAIL|SMS", message = "Canal de envio inválido.")
        String canal
) {}
