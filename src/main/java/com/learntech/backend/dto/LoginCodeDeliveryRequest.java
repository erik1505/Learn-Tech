package com.learntech.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginCodeDeliveryRequest(
	@NotBlank(message = "O desafio de login é obrigatório")
	String challengeId,

	@NotBlank(message = "Escolha um canal de envio")
	@Pattern(regexp = "EMAIL|SMS", message = "Canal de envio inválido")
	String canal
) {}
