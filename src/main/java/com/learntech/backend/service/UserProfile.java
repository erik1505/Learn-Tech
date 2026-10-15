package com.learntech.backend.service;

import com.learntech.backend.model.Role;

/** Visao unificada de Usuário + Professor/Aluno. perfilId = ID_PROFESSOR ou RA. */
public record UserProfile(Integer usuarioId, Integer perfilId, String nome, String email,
                          String telefone, Role role) {}
