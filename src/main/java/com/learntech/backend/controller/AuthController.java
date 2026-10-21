package com.learntech.backend.controller;

import com.learntech.backend.dto.LoginRequest;
import com.learntech.backend.dto.LoginCodeDeliveryRequest;
import com.learntech.backend.dto.LoginChallengeResponse;
import com.learntech.backend.dto.EmailCodeRequest;
import com.learntech.backend.dto.AuthChallengeResponse;
import com.learntech.backend.dto.RedefinirSenhaRequest;
import com.learntech.backend.dto.RegisterRequest;
import com.learntech.backend.dto.SolicitarRedefinicaoRequest;
import com.learntech.backend.service.UserProfile;
import com.learntech.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<UserProfile> cadastrar(@Valid @RequestBody RegisterRequest request) {
        UserProfile usuarioCriado = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioCriado);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginChallengeResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginChallengeResponse challenge = userService.iniciarLogin(request.email(), request.senha());
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/login/enviar-codigo")
    public ResponseEntity<AuthChallengeResponse> enviarCodigoLogin(
            @Valid @RequestBody LoginCodeDeliveryRequest request) {
        return ResponseEntity.ok(userService.enviarCodigoLogin(request.challengeId(), request.canal()));
    }

    @PostMapping("/login/verificar-codigo")
    public ResponseEntity<UserProfile> verificarCodigoLogin(@Valid @RequestBody EmailCodeRequest request) {
        UserProfile usuario = userService.verificarCodigoLogin(request.challengeId(), request.codigo());
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/redefinir-senha/codigo")
    public ResponseEntity<AuthChallengeResponse> solicitarRedefinicaoSenha(
            @Valid @RequestBody SolicitarRedefinicaoRequest request) {
        AuthChallengeResponse challenge = userService.solicitarRedefinicaoSenha(request.email());
        return ResponseEntity.ok(challenge);
    }

    @PutMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        userService.redefinirSenha(request.challengeId(), request.codigo(), request.novaSenha());
        return ResponseEntity.ok(Collections.singletonMap("message", "Senha redefinida com sucesso."));
    }
}