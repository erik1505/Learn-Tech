package com.learntech.backend.controller;

import com.learntech.backend.dto.TurmaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.service.TurmaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turmas")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @GetMapping
    public ResponseEntity<List<Responses.TurmaResponse>> listar() {
        return ResponseEntity.ok(turmaService.listarTodas());
    }

    @PostMapping
    public ResponseEntity<Responses.TurmaResponse> criar(@Valid @RequestBody TurmaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turmaService.criar(request));
    }

    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Responses.TurmaResponse>> turmasDoProfessor(@PathVariable Integer professorId) {
        return ResponseEntity.ok(turmaService.listarPorProfessor(professorId));
    }

    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<Responses.TurmaResponse>> turmasDoAluno(@PathVariable Integer alunoId) {
        return ResponseEntity.ok(turmaService.listarPorAluno(alunoId));
    }
}