package com.learntech.backend.repository;

import com.learntech.backend.model.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Integer> {
    List<Atividade> findByDisciplinaId(Integer disciplinaId);
    List<Atividade> findByDisciplinaTurmaId(Integer turmaId);
    List<Atividade> findByDisciplinaTurmaProfessorId(Integer professorId);
}
