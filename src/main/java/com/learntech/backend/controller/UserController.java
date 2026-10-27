package com.learntech.backend.controller;

import com.learntech.backend.dto.ProfileUpdateRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }

    /** {id} = ID_usuario (o "id" devolvido no login). */
    @GetMapping("/{id}")
    public Responses.UserResponse get(@PathVariable Integer id) {
        service.requireCurrentUser(id);
        return Responses.of(service.profileOf(id));
    }

    @PutMapping("/{id}")
    public Responses.UserResponse update(@PathVariable Integer id, @Valid @RequestBody ProfileUpdateRequest request) {
        service.requireCurrentUser(id);
        return Responses.of(service.update(id, request));
    }
}
