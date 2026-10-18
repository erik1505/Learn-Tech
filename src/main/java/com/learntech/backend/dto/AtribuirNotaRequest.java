package com.learntech.backend.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AtribuirNotaRequest(
        @NotNull(message = "A nota é obrigatória.")
        @DecimalMin(value = "0.0", message = "A nota mínima é 0.")
        @DecimalMax(value = "10.0", message = "A nota máxima é 10.")
        Double nota
) {}
