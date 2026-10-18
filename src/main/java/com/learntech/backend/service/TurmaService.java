package com.learntech.backend.service;

import com.learntech.backend.dto.TurmaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Matricula;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.MatriculaRepository;
import com.learntech.backend.repository.TurmaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculas;
    private final UserService userService;

    public TurmaService(TurmaRepository turmaRepository, MatriculaRepository matriculas, UserService userService) {
        this.turmaRepository = turmaRepository;
        this.matriculas = matriculas;
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

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarDisponiveisParaAluno(Integer alunoId) {
        Aluno aluno = userService.requireAluno(alunoId);
        Set<Integer> turmasMatriculadas = matriculas.findByAlunoRa(aluno.getRa()).stream()
                .map(matricula -> matricula.getTurma().getId())
                .collect(Collectors.toSet());
        return turmaRepository.findAll().stream()
                .filter(turma -> !turmasMatriculadas.contains(turma.getId()))
                .map(Responses::of)
                .toList();
    }

    @Transactional
    public Responses.TurmaResponse matricularAluno(Integer turmaId, Integer alunoId) {
        Aluno aluno = userService.requireAluno(alunoId);
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        if (!matriculas.existsByAlunoRaAndTurmaId(aluno.getRa(), turma.getId())) {
            Matricula matricula = new Matricula();
            matricula.setAluno(aluno);
            matricula.setTurma(turma);
            matricula.setStatus("ATIVA");
            matriculas.save(matricula);
        }
        return Responses.of(turma);
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