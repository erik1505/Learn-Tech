package com.learntech.backend.controller;

import com.learntech.backend.dto.AtividadeRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.ActivityStatus;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.Disciplina;
import com.learntech.backend.model.Turma;
import com.learntech.backend.model.Role;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.DisciplinaRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.service.NotificationService;
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
    private final NotificationService notifications;

    public AtividadeController(AtividadeRepository repository, DisciplinaRepository disciplinas,
                               TurmaRepository turmas, UserService users, NotificationService notifications) {
        this.repository = repository;
        this.disciplinas = disciplinas;
        this.turmas = turmas;
        this.users = users;
        this.notifications = notifications;
    }

    @GetMapping
    public List<Responses.AtividadeResponse> all() {
        var profile = users.currentProfile();
        List<Atividade> activities = profile.role() == Role.PROFESSOR
                ? repository.findByDisciplinaTurmaProfessorId(profile.perfilId())
                : repository.findByDisciplinaTurmaMatriculasAlunoRa(profile.perfilId());
        return activities.stream().map(Responses::of).toList();
    }

    @GetMapping("/{id}")
    public Responses.AtividadeResponse get(@PathVariable Integer id) {
        Atividade atividade = find(id);
        users.requireActivityAccess(atividade, false);
        return Responses.of(atividade);
    }

    /** Atividades de todas as disciplinas da turma. */
    @GetMapping("/turma/{id}")
    public List<Responses.AtividadeResponse> byClass(@PathVariable Integer id) {
        users.requireTurmaAccess(id, false);
        return repository.findByDisciplinaTurmaId(id).stream().map(Responses::of).toList();
    }

    @GetMapping("/disciplina/{id}")
    public List<Responses.AtividadeResponse> bySubject(@PathVariable Integer id) {
        users.requireDisciplinaAccess(disciplinas.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Disciplina não encontrada")));
        return repository.findByDisciplinaId(id).stream().map(Responses::of).toList();
    }

    @PostMapping
    public Responses.AtividadeResponse create(@RequestParam Integer professorId, @RequestBody AtividadeRequest r) {
        users.requireProfessor(professorId);
        Responses.required(r.turmaId(), "turmaId");
        users.requireTurmaAccess(r.turmaId(), true);
        Atividade a = new Atividade();
        apply(a, r);
        Atividade saved = repository.save(a);
        notifications.notifyActivity(saved.getDisciplina().getTurma().getId(), saved, true);
        return Responses.of(saved);
    }

    @PutMapping("/{id}")
    public Responses.AtividadeResponse update(@PathVariable Integer id, @RequestParam Integer professorId, @RequestBody AtividadeRequest r) {
        users.requireProfessor(professorId);
        Atividade a = find(id);
        users.requireActivityAccess(a, true);
        users.requireTurmaAccess(r.turmaId(), true);
        apply(a, r);
        Atividade saved = repository.save(a);
        notifications.notifyActivity(saved.getDisciplina().getTurma().getId(), saved, false);
        return Responses.of(saved);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @RequestParam Integer professorId) {
        users.requireProfessor(professorId);
        Atividade atividade = find(id);
        users.requireActivityAccess(atividade, true);
        repository.delete(atividade);
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
                    List<Disciplina> existentes = disciplinas.findByTurmaId(turma.getId());
                    if (existentes.size() == 1) return existentes.get(0);
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
