package com.learntech.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class LocalSmsGatewayService {
    private static final Logger logger = LoggerFactory.getLogger(LocalSmsGatewayService.class);
    private final RestClient restClient;
    private final String endpoint;
    private final String username;
    private final String password;

    public LocalSmsGatewayService(RestClient.Builder restClientBuilder,
                                  @Value("${app.sms-gateway.url:}") String endpoint,
                                  @Value("${app.sms-gateway.username:}") String username,
                                  @Value("${app.sms-gateway.password:}") String password) {
        this.restClient = restClientBuilder.build();
        this.endpoint = endpoint.trim();
        this.username = username;
        this.password = password;
    }

    public boolean configurado() {
        if (endpoint.isBlank() || username.isBlank() || password.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(endpoint);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException erro) {
            return false;
        }
    }

    public void enviarCodigo(String telefone, String mensagem) {
        if (!configurado()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gateway local de SMS não configurado. Consulte EMAIL-SETUP.md.");
        }

        String digits = telefone == null ? "" : telefone.replaceAll("\\D+", "");
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            digits = digits.substring(2);
        }
        if (!digits.matches("[1-9][0-9]{9,10}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Telefone inválido para envio por SMS.");
        }

        Map<String, Object> payload = Map.of(
                "textMessage", Map.of("text", mensagem),
                "phoneNumbers", List.of("+55" + digits)
        );

        try {
            restClient.post()
                    .uri(endpoint)
                    .headers(headers -> headers.setBasicAuth(username, password))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException erro) {
            logger.error("Falha ao enviar código pelo gateway SMS local.", erro);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o SMS. Verifique a conexão com o celular gateway.", erro);
        }
    }
}