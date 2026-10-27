package com.learntech.backend.service;

import com.learntech.backend.dto.TurmaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Matricula;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.MatriculaRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.repository.VinculoEscolaRepository;
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
    private final VinculoEscolaRepository vinculosEscola;

    public TurmaService(TurmaRepository turmaRepository, MatriculaRepository matriculas, UserService userService,
                        VinculoEscolaRepository vinculosEscola) {
        this.turmaRepository = turmaRepository;
        this.matriculas = matriculas;
        this.userService = userService;
        this.vinculosEscola = vinculosEscola;
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarTodas() {
        var current = userService.currentProfile();
        List<Turma> turmas = current.role() == com.learntech.backend.model.Role.PROFESSOR
                ? turmaRepository.findByProfessorId(current.perfilId())
                : turmaRepository.findByAlunoId(current.perfilId());
        return turmas.stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarPorProfessor(Integer professorId) {
        userService.requireProfessor(professorId);
        return turmaRepository.findByProfessorId(professorId).stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarPorAluno(Integer alunoId) {
        userService.requireAluno(alunoId);
        return turmaRepository.findByAlunoId(alunoId).stream().map(Responses::of).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarDisponiveisParaAluno(Integer alunoId) {
        Aluno aluno = userService.requireAluno(alunoId);
        List<Integer> escolaIds = userService.currentSchoolIds();
        Set<Integer> turmasMatriculadas = matriculas.findByAlunoRa(aluno.getRa()).stream()
                .map(matricula -> matricula.getTurma().getId())
                .collect(Collectors.toSet());
        List<Turma> turmasVisiveis = escolaIds.isEmpty()
                ? turmaRepository.findByEscolaIsNull()
                : turmaRepository.findByEscolaIdIn(escolaIds);
        return turmasVisiveis.stream()
                .filter(turma -> !turmasMatriculadas.contains(turma.getId()))
                .map(Responses::of)
                .toList();
    }

    @Transactional
    public Responses.TurmaResponse matricularAluno(Integer turmaId, Integer alunoId) {
        Aluno aluno = userService.requireAluno(alunoId);
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));

        if (turma.getEscola() != null && !userService.belongsToSchool(turma.getEscola().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Esta turma pertence a uma escola diferente.");
        }
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
        userService.requireProfessor(request.professorId());
        Turma turma = new Turma();
        turma.setQuantidade(request.quantidade());
        turma.setSala(request.sala());
        turma.setLinkGrupo(request.linkGrupo());
        turma.setProfessor(userService.requireProfessor(request.professorId()));
        return Responses.of(turmaRepository.save(turma));
    }
}