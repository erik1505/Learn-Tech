package com.learntech.backend.service;

import com.learntech.backend.model.Atividade;
import com.learntech.backend.repository.AtividadeRepository;
import com.learntech.backend.repository.MatriculaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class DeadlineReminderScheduler {
    private static final Logger logger = LoggerFactory.getLogger(DeadlineReminderScheduler.class);

    private final AtividadeRepository atividades;
    private final MatriculaRepository matriculas;
    private final DeadlineReminderProcessor processor;
    private final String timeZone;

    public DeadlineReminderScheduler(AtividadeRepository atividades, MatriculaRepository matriculas,
                                     DeadlineReminderProcessor processor,
                                     @Value("${app.reminders.time-zone:America/Sao_Paulo}") String timeZone) {
        this.atividades = atividades;
        this.matriculas = matriculas;
        this.processor = processor;
        this.timeZone = timeZone;
    }

    @Scheduled(
            cron = "${app.reminders.cron:0 0 8 * * *}",
            zone = "${app.reminders.time-zone:America/Sao_Paulo}")
    public void enviarLembretesDiarios() {
        LocalDate hoje = LocalDate.now(ZoneId.of(timeZone));
        var fim = hoje.plusDays(5).atStartOfDay();

        for (Atividade atividade : atividades.findByPrazoLessThan(fim)) {
            Integer turmaId = atividade.getDisciplina().getTurma().getId();
            for (Integer alunoRa : matriculas.findDistinctAlunoRasByTurmaIdAndStatusIgnoreCase(
                    turmaId, "ATIVA")) {
                try {
                    processor.enviarSeNecessario(atividade.getId(), alunoRa, hoje);
                } catch (RuntimeException erro) {
                    logger.error("Falha ao enviar lembrete da atividade {} para o aluno {} em {}.",
                            atividade.getId(), alunoRa, hoje, erro);
                }
            }
        }
    }
}
