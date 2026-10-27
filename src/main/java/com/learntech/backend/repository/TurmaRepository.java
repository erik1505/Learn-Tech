package com.learntech.backend.repository;

import com.learntech.backend.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TurmaRepository extends JpaRepository<Turma, Integer> {

    List<Turma> findByProfessorId(Integer professorId);
    Optional<Turma> findByEscolaIdAndCodigoExterno(Integer escolaId, String codigoExterno);
    List<Turma> findByEscolaIdIn(List<Integer> escolaIds);
    List<Turma> findByEscolaIsNull();

    @Query("SELECT m.turma FROM Matricula m WHERE m.aluno.ra = :alunoId")
    List<Turma> findByAlunoId(@Param("alunoId") Integer alunoId);
}