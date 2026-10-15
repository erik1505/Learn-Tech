package com.learntech.backend.controller;

import com.learntech.backend.dto.LoginRequest;
import com.learntech.backend.dto.RedefinirSenhaRequest;
import com.learntech.backend.dto.RegisterRequest;
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
    public ResponseEntity<UserProfile> login(@Valid @RequestBody LoginRequest request) {
        UserProfile usuario = userService.login(request.email(), request.senha());
        return ResponseEntity.ok(usuario);
    }

    @PutMapping("/redefinir-senha")
    public ResponseEntity<Map<String, String>> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        userService.redefinirSenha(request.email(), request.novaSenha());
        return ResponseEntity.ok(Collections.singletonMap("message", "Senha redefinida com sucesso."));
    }
}