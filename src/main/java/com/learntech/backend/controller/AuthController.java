package com.learntech.backend.controller;

import com.learntech.backend.dto.LoginRequest;
import com.learntech.backend.dto.LoginCodeDeliveryRequest;
import com.learntech.backend.dto.LoginChallengeResponse;
import com.learntech.backend.dto.LoginSessionResponse;
import com.learntech.backend.dto.EmailCodeRequest;
import com.learntech.backend.dto.AuthChallengeResponse;
import com.learntech.backend.dto.RedefinirSenhaRequest;
import com.learntech.backend.dto.RegisterRequest;
import com.learntech.backend.dto.SolicitarRedefinicaoRequest;
import com.learntech.backend.service.UserProfile;
import com.learntech.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<LoginChallengeResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginChallengeResponse challenge = userService.iniciarLogin(
                request.email(), request.senha(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(challenge);
    }

    @PostMapping("/login/enviar-codigo")
    public ResponseEntity<AuthChallengeResponse> enviarCodigoLogin(
            @Valid @RequestBody LoginCodeDeliveryRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(userService.enviarCodigoLogin(
                request.challengeId(), request.canal(), httpRequest.getRemoteAddr()));
    }

    @PostMapping("/login/verificar-codigo")
    public ResponseEntity<LoginSessionResponse> verificarCodigoLogin(@Valid @RequestBody EmailCodeRequest request) {
        LoginSessionResponse usuario = userService.verificarCodigoLogin(request.challengeId(), request.codigo());
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        userService.logout(authorization.substring("Bearer ".length()).trim());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/redefinir-senha/codigo")
    public ResponseEntity<AuthChallengeResponse> solicitarRedefinicaoSenha(
            @Valid @RequestBody SolicitarRedefinicaoRequest request,
            HttpServletRequest httpRequest) {
        AuthChallengeResponse challenge = userService.solicitarRedefinicaoSenha(
                request.email(), request.canal(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(challenge);
    }

    @PutMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        userService.redefinirSenha(request.challengeId(), request.codigo(), request.novaSenha());
        return ResponseEntity.ok(Collections.singletonMap("message", "Senha redefinida com sucesso."));
    }
}