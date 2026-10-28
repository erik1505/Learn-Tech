package com.learntech.backend.repository;

import com.learntech.backend.model.LembreteEntrega;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface LembreteEntregaRepository extends JpaRepository<LembreteEntrega, Integer> {
    boolean existsByAtividadeIdAndAlunoRaAndDataLembrete(
            Integer atividadeId, Integer alunoRa, LocalDate dataLembrete);
}
