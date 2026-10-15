package com.learntech.backend.dto;

import jakarta.validation.constraints.NotNull;

public record NotaRequest(
        @NotNull(message = "O ID da atividade é obrigatório.")
        Integer atividadeId,

        @NotNull(message = "O ID do aluno é obrigatório.")
        Integer alunoId,

        @NotNull(message = "A nota é obrigatória.")
        Double nota
) {}