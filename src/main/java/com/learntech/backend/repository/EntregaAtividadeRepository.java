package com.learntech.backend.repository;

import com.learntech.backend.model.EntregaAtividade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EntregaAtividadeRepository extends JpaRepository<EntregaAtividade, Integer> {

    List<EntregaAtividade> findByAtividadeId(Integer atividadeId);

    Optional<EntregaAtividade> findByAtividadeIdAndAlunoRa(Integer atividadeId, Integer ra);

    List<EntregaAtividade> findByAlunoRa(Integer ra);
}