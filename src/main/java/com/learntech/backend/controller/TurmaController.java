package com.learntech.backend.controller;

import com.learntech.backend.model.Turma;
import com.learntech.backend.service.TurmaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/turmas")
@CrossOrigin(origins = "*")
public class TurmaController {

    private final TurmaService turmaService;

    public TurmaController(TurmaService turmaService) {
        this.turmaService = turmaService;
    }

    @GetMapping
    public ResponseEntity<List<Turma>> listar() {
        return ResponseEntity.ok(turmaService.listarTodas());
    }

    @GetMapping("/professor/{professorId}")
    public ResponseEntity<List<Turma>> turmasDoProfessor(@PathVariable Integer professorId) {
        return ResponseEntity.ok(turmaService.listarPorProfessor(professorId));
    }

    @GetMapping("/aluno/{alunoId}")
    public ResponseEntity<List<Turma>> turmasDoAluno(@PathVariable Integer alunoId) {
        return ResponseEntity.ok(turmaService.listarPorAluno(alunoId));
    }
}