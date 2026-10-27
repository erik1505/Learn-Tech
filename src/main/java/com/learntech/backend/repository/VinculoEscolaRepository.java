package com.learntech.backend.repository;

import com.learntech.backend.model.VinculoEscola;
import com.learntech.backend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VinculoEscolaRepository extends JpaRepository<VinculoEscola, Integer> {
    Optional<VinculoEscola> findByEscolaIdAndCodigoExternoAndRole(
            Integer escolaId, String codigoExterno, Role role);
    Optional<VinculoEscola> findByEscolaIdAndUsuarioId(Integer escolaId, Integer usuarioId);
    List<VinculoEscola> findByUsuarioId(Integer usuarioId);
}
