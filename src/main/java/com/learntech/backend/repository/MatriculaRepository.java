package com.learntech.backend.repository;

import com.learntech.backend.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    List<Matricula> findByAlunoRa(Integer ra);
    List<Matricula> findByTurmaId(Integer turmaId);
    List<Matricula> findByTurmaIdAndStatusIgnoreCase(Integer turmaId, String status);
    boolean existsByAlunoRaAndTurmaId(Integer ra, Integer turmaId);
    boolean existsByAlunoRaAndTurmaIdAndStatusIgnoreCase(Integer ra, Integer turmaId, String status);

    @Query("SELECT DISTINCT m.aluno.ra FROM Matricula m WHERE m.turma.id = :turmaId AND LOWER(m.status) = LOWER(:status)")
    List<Integer> findDistinctAlunoRasByTurmaIdAndStatusIgnoreCase(
            @Param("turmaId") Integer turmaId, @Param("status") String status);
}
