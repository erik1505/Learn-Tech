package com.learntech.backend.dto;

import com.learntech.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String senha,

        @NotNull(message = "O perfil (role) é obrigatório")
        Role role,

        @NotBlank(message = "O telefone é obrigatório")
        @Pattern(regexp = "[1-9][0-9]{9,10}", message = "Informe um telefone com DDD e 10 ou 11 dígitos")
        String telefone
) {}