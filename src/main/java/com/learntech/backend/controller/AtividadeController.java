package com.learntech.backend.controller;

import com.learntech.backend.dto.AtividadeRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.ActivityStatus;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.Disciplina;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.DisciplinaRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/atividades")
@Transactional
public class AtividadeController {
    private final AtividadeRepository repository;
    private final DisciplinaRepository disciplinas;
    private final TurmaRepository turmas;
    private final UserService users;

    public AtividadeController(AtividadeRepository repository, DisciplinaRepository disciplinas,
                               TurmaRepository turmas, UserService users) {
        this.repository = repository;
        this.disciplinas = disciplinas;
        this.turmas = turmas;
        this.users = users;
    }

    @GetMapping
    public List<Responses.AtividadeResponse> all() { return repository.findAll().stream().map(Responses::of).toList(); }

    @GetMapping("/{id}")
    public Responses.AtividadeResponse get(@PathVariable Integer id) { return Responses.of(find(id)); }

    /** Atividades de todas as disciplinas da turma. */
    @GetMapping("/turma/{id}")
    public List<Responses.AtividadeResponse> byClass(@PathVariable Integer id) {
        return repository.findByDisciplinaTurmaId(id).stream().map(Responses::of).toList();
    }

    @GetMapping("/disciplina/{id}")
    public List<Responses.AtividadeResponse> bySubject(@PathVariable Integer id) {
        return repository.findByDisciplinaId(id).stream().map(Responses::of).toList();
    }

    @PostMapping
    public Responses.AtividadeResponse create(@RequestParam Integer professorId, @RequestBody AtividadeRequest r) {
        users.requireProfessor(professorId);
        Atividade a = new Atividade();
        apply(a, r);
        return Responses.of(repository.save(a));
    }

    @PutMapping("/{id}")
    public Responses.AtividadeResponse update(@PathVariable Integer id, @RequestParam Integer professorId, @RequestBody AtividadeRequest r) {
        users.requireProfessor(professorId);
        Atividade a = find(id);
        apply(a, r);
        return Responses.of(repository.save(a));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @RequestParam Integer professorId) {
        users.requireProfessor(professorId);
        repository.delete(find(id));
    }

    private Atividade find(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
    }

    private void apply(Atividade a, AtividadeRequest r) {
        Responses.required(r.titulo(), "titulo");
        Responses.required(r.descricao(), "descricao");
        Responses.required(r.prazo(), "prazo");
        Responses.required(r.turmaId(), "turmaId");
        Turma turma = turmas.findById(r.turmaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        Disciplina d = disciplinas.findFirstByTurmaIdAndNome(turma.getId(), turma.getSala())
                .orElseGet(() -> {
                    Disciplina nova = new Disciplina();
                    nova.setNome(turma.getSala());
                    nova.setStatus("ATIVA");
                    nova.setTurma(turma);
                    return disciplinas.save(nova);
                });
        a.setNome(r.titulo()); a.setDescricao(r.descricao()); a.setPrazo(r.prazo());
        a.setStatus(r.status() == null ? ActivityStatus.A_FAZER : r.status());
        a.setDisciplina(d);
    }
}
