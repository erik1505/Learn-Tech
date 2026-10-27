package com.learntech.backend.controller;

import com.learntech.backend.dto.NotaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.EntregaAtividade;
import com.learntech.backend.repository.EntregaAtividadeRepository;
import com.learntech.backend.repository.AlunoRepository;
import com.learntech.backend.service.NotificationService;
import com.learntech.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** A nota não tem tabela própria: fica em Entrega_atividade.Nota. */
@RestController
@RequestMapping("/api/notas")
@Transactional
public class NotaController {

    private final EntregaAtividadeRepository entregas;
    private final UserService users;
    private final NotificationService notifications;
    private final AlunoRepository alunos;

    public NotaController(EntregaAtividadeRepository entregas, UserService users,
                          NotificationService notifications, AlunoRepository alunos) {
        this.entregas = entregas;
        this.users = users;
        this.notifications = notifications;
        this.alunos = alunos;
    }

    /** {id} = ID_usuario do aluno. */
    @GetMapping("/aluno/{id}")
    public List<Responses.EntregaResponse> byStudent(@PathVariable Integer id) {
        Aluno aluno = users.requireAluno(id);
        return entregas.findByAlunoRa(aluno.getRa())
                .stream()
                .map(e -> Responses.of(e))
                .toList();
    }

    @PostMapping
    public Responses.EntregaResponse create(@RequestParam Integer professorId, @RequestBody NotaRequest r) {
        users.requireProfessor(professorId);
        Responses.required(r.atividadeId(), "atividadeId");
        Responses.required(r.alunoId(), "alunoId");
        Responses.required(r.nota(), "nota");

        Aluno aluno = alunos.findById(r.alunoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado"));
        EntregaAtividade e = entregas.findByAtividadeIdAndAlunoRa(r.atividadeId(), aluno.getRa())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "O aluno ainda não enviou esta atividade"));
        users.requireActivityAccess(e.getAtividade(), true);
        users.requireStudentEnrollmentForCurrentTeacher(
                e.getAtividade().getDisciplina().getTurma().getId(), aluno.getRa());

        e.setNota(r.nota());
        EntregaAtividade saved = entregas.save(e);

        notifications.notifyGrade(e.getAluno(), e.getAtividade(), r.nota());

        return Responses.of(saved);
    }
}