package com.learntech.backend.service;

import com.learntech.backend.dto.EntregaDetalheDTO;
import com.learntech.backend.dto.EntregaRequest;
import com.learntech.backend.dto.Responses;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.EntregaAtividade;
import com.learntech.backend.model.Priority;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.EntregaAtividadeRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EntregaService {

    private final EntregaAtividadeRepository entregaAtividadeRepository;
    private final AtividadeRepository atividadeRepository;
    private final UserService userService;
    private final NotificationService notifications;

    public EntregaService(EntregaAtividadeRepository entregaAtividadeRepository,
                          AtividadeRepository atividadeRepository,
                          UserService userService,
                          NotificationService notifications) {
        this.entregaAtividadeRepository = entregaAtividadeRepository;
        this.atividadeRepository = atividadeRepository;
        this.userService = userService;
        this.notifications = notifications;
    }

    @Transactional
    public void realizarEntrega(Integer atividadeId, Integer alunoId, EntregaRequest request) {
        Aluno aluno = userService.requireAluno(alunoId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        userService.requireActivityAccess(atividade, false);

        EntregaAtividade entrega = obterOuCriarEntrega(atividade, aluno);

        entrega.setTextoComentario(request.textoComentario());
        entrega.setArquivoEnviado(request.arquivoUrl());
        entrega.setDataEnvio(LocalDateTime.now());
        entrega.setStatusKanban("CONCLUIDA");

        entregaAtividadeRepository.save(entrega);
    }

    @Transactional
    public void iniciarAtividade(Integer atividadeId, Integer alunoId) {
        Aluno aluno = userService.requireAluno(alunoId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        userService.requireActivityAccess(atividade, false);
        EntregaAtividade entrega = obterOuCriarEntrega(atividade, aluno);
        if (entrega.foiEnviada()) {
            entrega.setStatusKanban("CONCLUIDA");
            entregaAtividadeRepository.save(entrega);
        } else {
            entrega.setStatusKanban("EM_ANDAMENTO");
            entrega.setDataEnvio(LocalDateTime.now());
            entregaAtividadeRepository.save(entrega);
        }
    }

    @Transactional
    public void sairAtividade(Integer atividadeId, Integer alunoId) {
        userService.requireAluno(alunoId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        userService.requireActivityAccess(atividade, false);
    }

    @Transactional
    public Responses.EntregaResponse atualizarPrioridade(
            Integer atividadeId, Integer alunoId, Priority prioridade) {
        Aluno aluno = userService.requireAluno(alunoId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        userService.requireActivityAccess(atividade, false);
        EntregaAtividade entrega = obterOuCriarEntrega(atividade, aluno);
        entrega.setPrioridade(prioridade.name());
        return Responses.of(entregaAtividadeRepository.save(entrega));
    }

    private EntregaAtividade obterOuCriarEntrega(Atividade atividade, Aluno aluno) {
        EntregaAtividade entrega = entregaAtividadeRepository
                .findByAtividadeIdAndAlunoRa(atividade.getId(), aluno.getRa())
                .orElseGet(EntregaAtividade::new);
        entrega.setAtividade(atividade);
        entrega.setAluno(aluno);
        return entrega;
    }

    @Transactional(readOnly = true)
    public List<EntregaDetalheDTO> buscarEntregasDetalhes(Integer atividadeId, Integer professorId) {
        userService.requireProfessor(professorId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));
        userService.requireActivityAccess(atividade, true);

        List<EntregaAtividade> entregas = entregaAtividadeRepository.findByAtividadeId(atividadeId).stream()
                .filter(EntregaAtividade::foiEnviada)
                .toList();

        return entregas.stream().map(e -> new EntregaDetalheDTO(
                e.getId(),
                e.getAluno().getRa(),
                e.getAluno().getNome(),
                e.getAluno().getEmail(),
                e.getTextoComentario(),
                e.getArquivoEnviado(),
                e.getNota(),
                e.getDataEnvio()
        )).toList();
    }

    @Transactional
    public void lancarNota(Integer entregaId, Integer professorId, Double nota) {
        userService.requireProfessor(professorId);

        EntregaAtividade entrega = entregaAtividadeRepository.findById(entregaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrega não encontrada"));
        userService.requireActivityAccess(entrega.getAtividade(), true);
        userService.requireStudentEnrollmentForCurrentTeacher(
                entrega.getAtividade().getDisciplina().getTurma().getId(), entrega.getAluno().getRa());

        entrega.setNota(nota);
        entregaAtividadeRepository.save(entrega);
        notifications.notifyGrade(entrega.getAluno(), entrega.getAtividade(), nota);
    }
}