package com.learntech.backend.repository;

import com.learntech.backend.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MatriculaRepository extends JpaRepository<Matricula, Integer> {
    List<Matricula> findByAlunoRa(Integer ra);
    List<Matricula> findByTurmaId(Integer turmaId);
    List<Matricula> findByTurmaIdAndStatusIgnoreCase(Integer turmaId, String status);
    boolean existsByAlunoRaAndTurmaId(Integer ra, Integer turmaId);
    boolean existsByAlunoRaAndTurmaIdAndStatusIgnoreCase(Integer ra, Integer turmaId, String status);
}
