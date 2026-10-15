package com.learntech.backend.service;

import com.learntech.backend.dto.TurmaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.TurmaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final UserService userService;

    public TurmaService(TurmaRepository turmaRepository, UserService userService) {
        this.turmaRepository = turmaRepository;
        this.userService = userService;
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarTodas() {
        return turmaRepository.findAll().stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarPorProfessor(Integer professorId) {
        return turmaRepository.findByProfessorId(professorId).stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarPorAluno(Integer alunoId) {
        return turmaRepository.findByAlunoId(alunoId).stream().map(Responses::of).toList();
    }

    @Transactional
    public Responses.TurmaResponse criar(TurmaRequest request) {
        Turma turma = new Turma();
        turma.setQuantidade(request.quantidade());
        turma.setSala(request.sala());
        turma.setLinkGrupo(request.linkGrupo());
        turma.setProfessor(userService.requireProfessor(request.professorId()));
        return Responses.of(turmaRepository.save(turma));
    }
}