package com.learntech.backend.repository;

import com.learntech.backend.model.Atividade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Integer> {
    List<Atividade> findByDisciplinaId(Integer disciplinaId);
    List<Atividade> findByDisciplinaTurmaId(Integer turmaId);
    List<Atividade> findByDisciplinaTurmaProfessorId(Integer professorId);

    @Query("SELECT DISTINCT a FROM Atividade a JOIN a.disciplina.turma t JOIN Matricula m ON m.turma = t WHERE m.aluno.ra = :alunoId AND m.status = 'ATIVA'")
    List<Atividade> findByDisciplinaTurmaMatriculasAlunoRa(@Param("alunoId") Integer alunoId);

    @EntityGraph(attributePaths = {"disciplina", "disciplina.turma"})
    List<Atividade> findByPrazoLessThan(LocalDateTime fim);
}
