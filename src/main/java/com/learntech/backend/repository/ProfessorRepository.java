package com.learntech.backend.repository;

import com.learntech.backend.model.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfessorRepository extends JpaRepository<Professor, Integer> {
    Optional<Professor> findByUsuarioId(Integer usuarioId);
    Optional<Professor> findByTelefone(String telefone);
}
