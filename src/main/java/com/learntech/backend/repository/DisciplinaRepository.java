package com.learntech.backend.repository;

import com.learntech.backend.model.Disciplina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DisciplinaRepository extends JpaRepository<Disciplina, Integer> {
    List<Disciplina> findByTurmaId(Integer turmaId);
    List<Disciplina> findByTurmaProfessorId(Integer professorId);
    Optional<Disciplina> findFirstByTurmaIdAndNome(Integer turmaId, String nome);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT d FROM Disciplina d JOIN d.turma t JOIN Matricula m ON m.turma = t WHERE m.aluno.ra = :alunoId AND m.status = 'ATIVA'")
    List<Disciplina> findByStudentEnrollment(@org.springframework.data.repository.query.Param("alunoId") Integer alunoId);
}
