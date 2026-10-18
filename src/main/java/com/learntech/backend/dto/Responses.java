package com.learntech.backend.dto;

import com.learntech.backend.model.*;
import com.learntech.backend.service.UserProfile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/** Respostas da API. Evita serializar entidades JPA (proxies lazy e o hash da senha). */
public final class Responses {
    private Responses() {}

    public static void required(Object value, String field) {
        if (value == null || (value instanceof String s && s.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo obrigatório: " + field);
        }
    }

    public record TurmaResponse(Integer id, Integer quantidade, String sala, String linkGrupo,
                                Integer professorId, Integer professorUsuarioId, String professorNome) {}
    public record DisciplinaResponse(Integer id, String nome, String status, Integer turmaId) {}
    public record AtividadeResponse(Integer id, String titulo, String descricao, LocalDateTime prazo,
                                    ActivityStatus status, Integer disciplinaId, String disciplinaNome, Integer turmaId) {}
    public record EntregaResponse(Integer id, Integer atividadeId, Integer alunoId, Integer ra, String arquivoUrl,
                                  LocalDateTime enviadaEm, Integer nota, Priority prioridade, ActivityStatus statusKanban, String alunoNome) {}
    public record NotificacaoResponse(Integer id, String titulo, String mensagem, String icone,
                                      boolean lida, LocalDateTime criadaEm) {}
    public record PostItResponse(Integer id, String titulo, String conteudo, Integer atividadeId) {}
    public record UserResponse(Integer id, Integer perfilId, String nome, String email, String telefone, Role role) {}

    public static TurmaResponse of(Turma t) {
        Professor p = t.getProfessor();
        return new TurmaResponse(t.getId(), t.getQuantidade(), t.getSala(), t.getLinkGrupo(), p.getId(),
                p.getUsuario() == null ? null : p.getUsuario().getId(), p.getNome());
    }

    public static DisciplinaResponse of(Disciplina d) {
        return new DisciplinaResponse(d.getId(), d.getNome(), d.getStatus(), d.getTurma().getId());
    }

    public static AtividadeResponse of(Atividade a) {
        Disciplina d = a.getDisciplina();
        return new AtividadeResponse(a.getId(), a.getNome(), a.getDescricao(), a.getPrazo(), a.getStatus(),
                d != null ? d.getId() : null,
                d != null ? d.getNome() : null,
                (d != null && d.getTurma() != null) ? d.getTurma().getId() : null);
    }

    public static EntregaResponse of(EntregaAtividade e) {
        Aluno al = e.getAluno();

        Integer notaInt = e.getNota() != null ? e.getNota().intValue() : null;

        Priority prio = Priority.MEDIA;
        try {
            if (e.getPrioridade() != null) {
                prio = Priority.valueOf(e.getPrioridade().toUpperCase());
            }
        } catch (Exception ignored) {}

        ActivityStatus statusK = ActivityStatus.A_FAZER;
        try {
            if (e.getStatusKanban() != null) {
                statusK = ActivityStatus.valueOf(e.getStatusKanban().toUpperCase());
            }
        } catch (Exception ignored) {}
        if (statusK == ActivityStatus.A_FAZER
                && ((e.getTextoComentario() != null && !e.getTextoComentario().isBlank())
                || (e.getArquivoEnviado() != null && !e.getArquivoEnviado().isBlank()))) {
            statusK = ActivityStatus.CONCLUIDA;
        }

        return new EntregaResponse(
                e.getId(),
                e.getAtividade() != null ? e.getAtividade().getId() : null,
                (al != null && al.getUsuario() != null) ? al.getUsuario().getId() : null,
                al != null ? al.getRa() : null,
                e.getArquivoEnviado(),
                e.getDataEnvio(),
                notaInt,
                prio,
                statusK,
                al != null ? al.getNome() : null
        );
    }

    public static NotificacaoResponse of(Notificacao n) {
        return new NotificacaoResponse(n.getId(), n.getTitulo(), n.getMensagem(), n.getIcone(), n.isLida(), n.getCriadaEm());
    }

    public static PostItResponse of(PostIt p) {
        return new PostItResponse(p.getId(), p.getTitulo(), p.getConteudo(), p.getAtividade() != null ? p.getAtividade().getId() : null);
    }

    public static UserResponse of(UserProfile u) {
        return new UserResponse(u.usuarioId(), u.perfilId(), u.nome(), u.email(), u.telefone(), u.role());
    }
}