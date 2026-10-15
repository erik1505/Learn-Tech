package com.learntech.backend.dto;

/** professorId = id do usuário professor (o mesmo "id" devolvido no login). */
public record TurmaRequest(Integer quantidade, String sala, String linkGrupo, Integer professorId) {}
