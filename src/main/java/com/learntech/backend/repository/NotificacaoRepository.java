package com.learntech.backend.repository;

import com.learntech.backend.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Integer> {
    List<Notificacao> findByUsuarioIdOrderByCriadaEmDesc(Integer usuarioId);
    long countByUsuarioIdAndLidaFalse(Integer usuarioId);
}
