package com.learntech.backend.service;

import com.learntech.backend.model.Role;

public record AuthenticatedUser(Integer usuarioId, Integer perfilId, String nome, String email,
                                String telefone, Role role) {}
