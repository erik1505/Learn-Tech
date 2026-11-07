package com.learntech.backend.service;

import com.learntech.backend.model.Role;

/** Visão unificada de Usuário + Professor/Aluno. perfilId = ID_PROFESSOR ou Id_aluno. */
public record UserProfile(Integer usuarioId, Integer perfilId, String nome, String email,
                          String telefone, String ra, Role role) {}
