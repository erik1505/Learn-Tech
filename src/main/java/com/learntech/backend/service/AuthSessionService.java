package com.learntech.backend.service;

import com.learntech.backend.model.AuthSession;
import com.learntech.backend.model.Usuario;
import com.learntech.backend.repository.AuthSessionRepository;
import com.learntech.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthSessionService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long SESSION_LIFETIME_HOURS = 12;

    private final AuthSessionRepository sessions;
    private final UsuarioRepository users;

    public AuthSessionService(AuthSessionRepository sessions, UsuarioRepository users) {
        this.sessions = sessions;
        this.users = users;
    }

    @Transactional
    public String create(Integer userId) {
        Usuario user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
        byte[] randomToken = new byte[32];
        RANDOM.nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        sessions.save(new AuthSession(hash(token), user, LocalDateTime.now().plusHours(SESSION_LIFETIME_HOURS)));
        return token;
    }

    @Transactional(readOnly = true)
    public AuthenticatedUser authenticate(String token, UserService userService) {
        AuthSession session = sessions.findByTokenHashAndExpiresAtAfter(hash(token), LocalDateTime.now())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada"));
        UserProfile profile = userService.profileOf(session.getUsuario().getId());
        return new AuthenticatedUser(profile.usuarioId(), profile.perfilId(), profile.nome(),
                profile.email(), profile.telefone(), profile.role());
    }

    @Transactional
    public void revoke(String token) {
        sessions.deleteById(hash(token));
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }
}
