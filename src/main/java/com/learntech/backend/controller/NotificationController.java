package com.learntech.backend.controller;

import com.learntech.backend.dto.Responses;
import com.learntech.backend.service.NotificationService;
import com.learntech.backend.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

/** userId = ID_usuario. */
@RestController
@RequestMapping("/api/notificacoes")
public class NotificationController {
    private final NotificationService service;
    private final UserService users;

    public NotificationController(NotificationService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @GetMapping("/usuario/{userId}")
    public List<Responses.NotificacaoResponse> list(@PathVariable Integer userId) {
        users.requireCurrentUser(userId);
        return service.list(userId).stream().map(Responses::of).toList();
    }

    @GetMapping("/usuario/{userId}/nao-lidas")
    public Map<String, Long> unread(@PathVariable Integer userId) {
        users.requireCurrentUser(userId);
        return Map.of("quantidade", service.unread(userId));
    }

    @PatchMapping("/{id}/ler")
    public Map<String, String> read(@PathVariable Integer id, @RequestParam Integer userId) {
        users.requireCurrentUser(userId);
        service.markRead(userId, id);
        return Map.of("status", "ok");
    }

    @PatchMapping("/ler-todas")
    public Map<String, String> readAll(@RequestParam Integer userId) {
        users.requireCurrentUser(userId);
        service.markAllRead(userId);
        return Map.of("status", "ok");
    }
}
