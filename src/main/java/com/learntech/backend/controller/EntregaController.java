package com.learntech.backend.controller;

import com.learntech.backend.dto.AtribuirNotaRequest;
import com.learntech.backend.dto.EntregaDetalheDTO;
import com.learntech.backend.dto.EntregaRequest;
import com.learntech.backend.dto.PrioridadeRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    @PostMapping("/atividade/{atividadeId}")
    public ResponseEntity<?> enviarEntrega(
            @PathVariable Integer atividadeId,
            @RequestParam Integer alunoId,
            @RequestBody EntregaRequest request) {
        entregaService.realizarEntrega(atividadeId, alunoId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Collections.singletonMap("message", "Entrega enviada com sucesso!"));
    }

    @PostMapping("/atividade/{atividadeId}/iniciar")
    public ResponseEntity<Void> iniciarAtividade(
            @PathVariable Integer atividadeId,
            @RequestParam Integer alunoId) {
        entregaService.iniciarAtividade(atividadeId, alunoId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/atividade/{atividadeId}/sair")
    public ResponseEntity<Void> sairAtividade(
            @PathVariable Integer atividadeId,
            @RequestParam Integer alunoId) {
        entregaService.sairAtividade(atividadeId, alunoId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/atividade/{atividadeId}/prioridade")
    public ResponseEntity<Responses.EntregaResponse> atualizarPrioridade(
            @PathVariable Integer atividadeId,
            @RequestParam Integer alunoId,
            @Valid @RequestBody PrioridadeRequest request) {
        return ResponseEntity.ok(entregaService.atualizarPrioridade(
                atividadeId, alunoId, request.prioridade()
        ));
    }

    @GetMapping("/atividade/{atividadeId}/detalhes")
    public ResponseEntity<List<EntregaDetalheDTO>> listarEntregasParaCorrecao(
            @PathVariable Integer atividadeId,
            @RequestParam Integer professorId) {
        return ResponseEntity.ok(entregaService.buscarEntregasDetalhes(atividadeId, professorId));
    }

    @PutMapping("/{entregaId}/nota")
    public ResponseEntity<Map<String, String>> atribuirNota(
            @PathVariable Integer entregaId,
            @RequestParam Integer professorId,
            @Valid @RequestBody AtribuirNotaRequest request) {
        entregaService.lancarNota(entregaId, professorId, request.nota());
        return ResponseEntity.ok(Collections.singletonMap("message", "Nota atribuída com sucesso!"));
    }
}