package com.learntech.backend.repository;

import com.learntech.backend.model.EntregaAtividade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaAtividadeRepository extends JpaRepository<EntregaAtividade, Integer> {

    @EntityGraph(attributePaths = {"aluno", "atividade"})
    List<EntregaAtividade> findByAtividadeId(Integer atividadeId);

    @EntityGraph(attributePaths = {"aluno", "atividade", "atividade.disciplina", "atividade.disciplina.turma"})
    List<EntregaAtividade> findByAtividadeDisciplinaTurmaId(Integer turmaId);

    Optional<EntregaAtividade> findByAtividadeIdAndAlunoRa(Integer atividadeId, String ra);

    List<EntregaAtividade> findByAlunoRa(String ra);
}