package com.learntech.backend.dto;

import com.learntech.backend.model.ActivityStatus;
import java.time.LocalDateTime;

public record AtividadeRequest(String titulo, String descricao, LocalDateTime prazo,
                               ActivityStatus status, Integer disciplinaId) {}
