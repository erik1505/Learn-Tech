package com.learntech.backend.dto;

import java.time.LocalDateTime;

public record EntregaDetalheDTO(
        Integer entregaId,
        Integer alunoId,
        String alunoNome,
        String alunoEmail,
        String textoComentario,
        String arquivoUrl,
        Double nota,
        LocalDateTime dataEnvio
) {}