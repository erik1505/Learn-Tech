package com.learntech.backend.controller;

import com.learntech.backend.dto.PostItRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.PostIt;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.PostItRepository;
import com.learntech.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Learn Book: post-its do aluno por atividade. alunoId = ID_usuario do aluno. */
@RestController
@RequestMapping("/api/postits")
@Transactional
public class PostItController {
    private final PostItRepository repository;
    private final AtividadeRepository atividades;
    private final UserService users;

    public PostItController(PostItRepository repository, AtividadeRepository atividades, UserService users) {
        this.repository = repository; this.atividades = atividades; this.users = users;
    }

    @GetMapping("/atividade/{atividadeId}")
    public List<Responses.PostItResponse> list(@PathVariable Integer atividadeId, @RequestParam Integer alunoId) {
        Aluno aluno = users.requireAluno(alunoId);
        Atividade atividade = atividades.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        users.requireActivityAccess(atividade, false);
        return repository.findByAlunoRaAndAtividadeId(aluno.getRa(), atividadeId).stream().map(Responses::of).toList();
    }

    @PostMapping("/atividade/{atividadeId}")
    public Responses.PostItResponse create(@PathVariable Integer atividadeId, @RequestParam Integer alunoId, @RequestBody PostItRequest r) {
        Aluno aluno = users.requireAluno(alunoId);
        Responses.required(r.titulo(), "titulo");
        Responses.required(r.conteudo(), "conteudo");
        Atividade atividade = atividades.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        users.requireActivityAccess(atividade, false);
        PostIt p = new PostIt();
        p.setTitulo(r.titulo()); p.setConteudo(r.conteudo()); p.setAtividade(atividade); p.setAluno(aluno);
        return Responses.of(repository.save(p));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id, @RequestParam Integer alunoId) {
        Aluno aluno = users.requireAluno(alunoId);
        PostIt p = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post-it não encontrado"));
        if (!p.getAluno().getRa().equals(aluno.getRa())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Post-it de outro aluno");
        repository.delete(p);
    }
}
