package com.learntech.backend.repository;

import com.learntech.backend.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {

    List<Turma> findByProfessorId(Integer professorId);

    @Query("SELECT m.turma FROM Matricula m WHERE m.aluno.ra = :alunoId")
    List<Turma> findByAlunoId(@Param("alunoId") Integer alunoId);
}