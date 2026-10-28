package com.learntech.backend.service;

import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.EntregaAtividade;
import com.learntech.backend.model.LembreteEntrega;
import com.learntech.backend.model.Priority;
import com.learntech.backend.repository.AlunoRepository;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.EntregaAtividadeRepository;
import com.learntech.backend.repository.LembreteEntregaRepository;
import com.learntech.backend.repository.MatriculaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
public class DeadlineReminderProcessor {
    private final AtividadeRepository atividades;
    private final AlunoRepository alunos;
    private final EntregaAtividadeRepository entregas;
    private final LembreteEntregaRepository lembretes;
    private final MatriculaRepository matriculas;
    private final NotificationService notifications;

    public DeadlineReminderProcessor(AtividadeRepository atividades, AlunoRepository alunos,
                                     EntregaAtividadeRepository entregas,
                                     LembreteEntregaRepository lembretes,
                                     MatriculaRepository matriculas,
                                     NotificationService notifications) {
        this.atividades = atividades;
        this.alunos = alunos;
        this.entregas = entregas;
        this.lembretes = lembretes;
        this.matriculas = matriculas;
        this.notifications = notifications;
    }

    @Transactional
    public boolean enviarSeNecessario(Integer atividadeId, Integer alunoRa, LocalDate hoje) {
        Atividade atividade = atividades.findById(atividadeId).orElse(null);
        Aluno aluno = alunos.findById(alunoRa).orElse(null);
        if (atividade == null || aluno == null || aluno.getUsuario() == null) {
            return false;
        }

        Integer turmaId = atividade.getDisciplina().getTurma().getId();
        if (!matriculas.existsByAlunoRaAndTurmaIdAndStatusIgnoreCase(alunoRa, turmaId, "ATIVA")) {
            return false;
        }

        EntregaAtividade entrega = entregas
                .findByAtividadeIdAndAlunoRa(atividadeId, alunoRa)
                .orElse(null);
        if (entrega != null && entrega.foiEnviada()) {
            return false;
        }

        Priority prioridade = entrega == null || entrega.getPrioridade() == null
                ? Priority.MEDIA
                : Priority.valueOf(entrega.getPrioridade().toUpperCase(Locale.ROOT));
        LocalDate prazo = atividade.getPrazo().toLocalDate();
        int diasAntes = switch (prioridade) {
            case BAIXA -> 1;
            case MEDIA -> 2;
            case ALTA -> 4;
        };
        if (hoje.isBefore(prazo.minusDays(diasAntes))) {
            return false;
        }
        if (lembretes.existsByAtividadeIdAndAlunoRaAndDataLembrete(atividadeId, alunoRa, hoje)) {
            return false;
        }

        lembretes.saveAndFlush(new LembreteEntrega(atividadeId, alunoRa, hoje));
        notifications.notifyDeadlineReminder(
                aluno, atividade, prioridade, ChronoUnit.DAYS.between(hoje, prazo));
        return true;
    }
}
