package com.learntech.backend.controller;

import com.learntech.backend.dto.DisciplinaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Disciplina;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.DisciplinaRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Novo: no SQL, a atividade pertence a uma Disciplina, que pertence a uma Turma. */
@RestController
@RequestMapping("/api/disciplinas")
@Transactional
public class DisciplinaController {
    private final DisciplinaRepository repository;
    private final TurmaRepository turmas;
    private final UserService users;

    public DisciplinaController(DisciplinaRepository repository, TurmaRepository turmas, UserService users) {
        this.repository = repository; this.turmas = turmas; this.users = users;
    }

    @GetMapping
    public List<Responses.DisciplinaResponse> all() { return repository.findAll().stream().map(Responses::of).toList(); }

    @GetMapping("/turma/{id}")
    public List<Responses.DisciplinaResponse> byClass(@PathVariable Integer id) {
        return repository.findByTurmaId(id).stream().map(Responses::of).toList();
    }

    @PostMapping
    public Responses.DisciplinaResponse create(@RequestParam Integer professorId, @RequestBody DisciplinaRequest r) {
        users.requireProfessor(professorId);
        Responses.required(r.nome(), "nome");
        Responses.required(r.turmaId(), "turmaId");
        Turma turma = turmas.findById(r.turmaId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        Disciplina d = new Disciplina();
        d.setNome(r.nome());
        d.setStatus(r.status() == null || r.status().isBlank() ? "ATIVA" : r.status());
        d.setTurma(turma);
        return Responses.of(repository.save(d));
    }
}
