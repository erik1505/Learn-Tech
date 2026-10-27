package com.learntech.backend.repository;

import com.learntech.backend.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EscolaRepository extends JpaRepository<Escola, Integer> {
    Optional<Escola> findByCodigoExterno(String codigoExterno);
}
