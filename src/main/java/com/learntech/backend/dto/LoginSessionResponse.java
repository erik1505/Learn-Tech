package com.learntech.backend.dto;

import com.learntech.backend.model.Role;
import com.learntech.backend.service.UserProfile;

public record LoginSessionResponse(String accessToken, Integer usuarioId, Integer perfilId,
                                   String nome, String email, String telefone, Role role) {
    public static LoginSessionResponse of(String accessToken, UserProfile user) {
        return new LoginSessionResponse(accessToken, user.usuarioId(), user.perfilId(),
                user.nome(), user.email(), user.telefone(), user.role());
    }
}
