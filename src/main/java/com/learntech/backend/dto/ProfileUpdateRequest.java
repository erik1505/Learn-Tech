package com.learntech.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(@NotBlank String nome, @NotBlank @Email String email) {}
