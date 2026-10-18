package com.learntech.backend.repository;

import com.learntech.backend.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Integer> {
    List<Disciplina> findByTurmaId(Integer turmaId);
    Optional<Disciplina> findFirstByTurmaIdAndNome(Integer turmaId, String nome);
}
