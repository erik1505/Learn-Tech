package com.learntech.backend.dto;

import com.learntech.backend.model.Priority;
import jakarta.validation.constraints.NotNull;

public record PrioridadeRequest(
        @NotNull(message = "A prioridade é obrigatória.")
        Priority prioridade
) {}
