package com.learntech.backend.service;

import com.learntech.backend.model.Notificacao;
import com.learntech.backend.model.Usuario;
import com.learntech.backend.repository.NotificacaoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class NotificationService {
    private final NotificacaoRepository repository;
    private final UserService users;

    public NotificationService(NotificacaoRepository repository, UserService users) {
        this.repository = repository;
        this.users = users;
    }

    public List<Notificacao> list(Integer usuarioId) { return repository.findByUsuarioIdOrderByCriadaEmDesc(usuarioId); }
    public long unread(Integer usuarioId) { return repository.countByUsuarioIdAndLidaFalse(usuarioId); }

    public Notificacao create(Integer usuarioId, String titulo, String mensagem, String icone) {
        Usuario user = users.get(usuarioId);
        Notificacao n = new Notificacao();
        n.setUsuario(user);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setIcone(icone == null ? "🔔" : icone);
        return repository.save(n);
    }

    @Transactional
    public void markRead(Integer usuarioId, Integer id) {
        Notificacao n = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        if (!n.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notificação não pertence ao usuário");
        }
        n.setLida(true);
        repository.save(n);
    }

    @Transactional
    public void markAllRead(Integer usuarioId) {
        List<Notificacao> all = list(usuarioId);
        all.forEach(n -> n.setLida(true));
        repository.saveAll(all);
    }
}
