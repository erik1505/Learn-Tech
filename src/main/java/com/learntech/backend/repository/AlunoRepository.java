package com.learntech.backend.repository;

import com.learntech.backend.model.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AlunoRepository extends JpaRepository<Aluno, Integer> {
    Optional<Aluno> findByUsuarioId(Integer usuarioId);
    Optional<Aluno> findByTelefone(String telefone);
}
