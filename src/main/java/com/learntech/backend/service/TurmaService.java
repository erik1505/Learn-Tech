package com.learntech.backend.service;

import com.learntech.backend.dto.TurmaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Matricula;
import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.MatriculaRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.repository.VinculoEscolaRepository;
import com.learntech.backend.model.Role;
import com.learntech.backend.repository.EntregaAtividadeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final MatriculaRepository matriculas;
    private final EntregaAtividadeRepository entregas;
    private final UserService userService;
    private final VinculoEscolaRepository vinculosEscola;

    public TurmaService(TurmaRepository turmaRepository, MatriculaRepository matriculas,
                        EntregaAtividadeRepository entregas, UserService userService,
                        VinculoEscolaRepository vinculosEscola) {
        this.turmaRepository = turmaRepository;
        this.matriculas = matriculas;
        this.entregas = entregas;
        this.userService = userService;
        this.vinculosEscola = vinculosEscola;
    }

    @Transactional
    public List<Responses.TurmaResponse> listarTodas() {
        var current = userService.currentProfile();
        boolean incluirConvites = current.role() == Role.PROFESSOR;
        List<Turma> turmas = incluirConvites
                ? turmaRepository.findByProfessorId(current.perfilId())
                : turmaRepository.findByAlunoId(current.perfilId());
        return turmas.stream().map(turma -> resposta(turma, incluirConvites)).toList();
    }

    @Transactional
    public List<Responses.TurmaResponse> listarPorProfessor(Integer professorId) {
        userService.requireProfessor(professorId);
        return turmaRepository.findByProfessorId(professorId).stream()
                .map(turma -> resposta(turma, true)).toList();
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaResponse> listarPorAluno(Integer alunoId) {
        userService.requireAluno(alunoId);
        return turmaRepository.findByAlunoId(alunoId).stream().map(Responses::of).toList();
    }

    @Transactional
    public Responses.TurmaResponse matricularPorConvite(String codigoConvite) {
        var current = userService.currentProfile();
        if (current.role() != Role.ALUNO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente alunos podem entrar em uma turma.");
        }
        Aluno aluno = userService.requireAluno(current.perfilId());
        Turma turma = turmaRepository.findByCodigoConvite(codigoConvite)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite inválido ou expirado."));
        if (!matriculas.existsByAlunoRaAndTurmaId(aluno.getRa(), turma.getId())) {
            Matricula matricula = new Matricula();
            matricula.setAluno(aluno);
            matricula.setTurma(turma);
            matricula.setStatus("ATIVA");
            matriculas.save(matricula);
            turma.setQuantidade(matriculas.findByTurmaIdAndStatusIgnoreCase(turma.getId(), "ATIVA").size());
            turmaRepository.save(turma);
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
        turma.gerarCodigoConviteSeNecessario();
        return Responses.of(turmaRepository.save(turma), true);
    }

    @Transactional(readOnly = true)
    public List<Responses.TurmaIntegranteResponse> listarIntegrantes(Integer turmaId) {
        userService.requireTurmaAccess(turmaId, true);

        var mediasPorAluno = entregas.findByAtividadeDisciplinaTurmaId(turmaId).stream()
                .filter(entrega -> entrega.getNota() != null && entrega.getAluno() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        entrega -> entrega.getAluno().getId(),
                        java.util.stream.Collectors.averagingDouble(entrega -> entrega.getNota())));

        return matriculas.findByTurmaIdAndStatusIgnoreCase(turmaId, "ATIVA").stream()
                .map(Matricula::getAluno)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .map(aluno -> new Responses.TurmaIntegranteResponse(
                        aluno.getNome(), aluno.getRa(), aluno.getEmail(), mediasPorAluno.get(aluno.getId())))
                .toList();
    }

    private Responses.TurmaResponse resposta(Turma turma, boolean incluirConvite) {
        if (incluirConvite && (turma.getCodigoConvite() == null || turma.getCodigoConvite().isBlank())) {
            turma.gerarCodigoConviteSeNecessario();
            turmaRepository.save(turma);
        }
        return Responses.of(turma, incluirConvite);
    }
}