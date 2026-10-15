package com.learntech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** professorId = ID do perfil de professor devolvido no login. */
public record TurmaRequest(
	@NotNull Integer quantidade,
	@NotBlank String sala,
	@NotBlank String linkGrupo,
	@NotNull Integer professorId
) {}
