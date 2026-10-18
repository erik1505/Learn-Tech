package com.learntech.backend.service;

import com.learntech.backend.dto.EntregaDetalheDTO;
import com.learntech.backend.dto.EntregaRequest;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.EntregaAtividade;
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

    public EntregaService(EntregaAtividadeRepository entregaAtividadeRepository,
                          AtividadeRepository atividadeRepository,
                          UserService userService) {
        this.entregaAtividadeRepository = entregaAtividadeRepository;
        this.atividadeRepository = atividadeRepository;
        this.userService = userService;
    }

    @Transactional
    public void realizarEntrega(Integer atividadeId, Integer alunoId, EntregaRequest request) {
        Aluno aluno = userService.requireAluno(alunoId);
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Atividade não encontrada"));

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
        EntregaAtividade entrega = obterOuCriarEntrega(atividade, aluno);
        if (foiEnviada(entrega)) {
            entrega.setStatusKanban("CONCLUIDA");
            entregaAtividadeRepository.save(entrega);
        } else {
            entrega.setStatusKanban("EM_ANDAMENTO");
            entrega.setDataEnvio(LocalDateTime.now());
            entregaAtividadeRepository.save(entrega);
        }
    }

    private EntregaAtividade obterOuCriarEntrega(Atividade atividade, Aluno aluno) {
        EntregaAtividade entrega = entregaAtividadeRepository
                .findByAtividadeIdAndAlunoRa(atividade.getId(), aluno.getRa())
                .orElseGet(EntregaAtividade::new);
        entrega.setAtividade(atividade);
        entrega.setAluno(aluno);
        return entrega;
    }

    private boolean foiEnviada(EntregaAtividade entrega) {
        if ("CONCLUIDA".equalsIgnoreCase(entrega.getStatusKanban())) {
            return true;
        }
        return "A_FAZER".equalsIgnoreCase(entrega.getStatusKanban())
                && ((entrega.getTextoComentario() != null && !entrega.getTextoComentario().isBlank())
                || (entrega.getArquivoEnviado() != null && !entrega.getArquivoEnviado().isBlank()));
    }

    public List<EntregaDetalheDTO> buscarEntregasDetalhes(Integer atividadeId, Integer professorId) {
        userService.requireProfessor(professorId);

        List<EntregaAtividade> entregas = entregaAtividadeRepository.findByAtividadeId(atividadeId).stream()
                .filter(this::foiEnviada)
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

        entrega.setNota(nota);
        entregaAtividadeRepository.save(entrega);
    }
}