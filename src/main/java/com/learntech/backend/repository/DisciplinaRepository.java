package com.learntech.backend.repository;

import com.learntech.backend.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Integer> {
    List<Disciplina> findByTurmaId(Integer turmaId);
}
