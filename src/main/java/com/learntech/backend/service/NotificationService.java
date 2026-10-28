package com.learntech.backend.service;

import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.Matricula;
import com.learntech.backend.model.Notificacao;
import com.learntech.backend.model.Priority;
import com.learntech.backend.model.Usuario;
import com.learntech.backend.repository.MatriculaRepository;
import com.learntech.backend.repository.NotificacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificacaoRepository repository;
    private final UserService users;
    private final MatriculaRepository matriculas;
    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String mailHost;

    public NotificationService(NotificacaoRepository repository, UserService users,
                               MatriculaRepository matriculas, JavaMailSender mailSender,
                               @Value("${app.mail.from}") String fromAddress,
                               @Value("${spring.mail.host:localhost}") String mailHost) {
        this.repository = repository;
        this.users = users;
        this.matriculas = matriculas;
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.mailHost = mailHost;
    }

    public List<Notificacao> list(Integer usuarioId) { return repository.findByUsuarioIdOrderByCriadaEmDesc(usuarioId); }
    public long unread(Integer usuarioId) { return repository.countByUsuarioIdAndLidaFalse(usuarioId); }

    public Notificacao create(Integer usuarioId, String titulo, String mensagem, String icone) {
        Usuario user = users.get(usuarioId);
        return create(user, titulo, mensagem, icone);
    }

    @Transactional
    public void notifyActivity(Integer turmaId, Atividade atividade, boolean criada) {
        String titulo = criada ? "Nova atividade" : "Atividade atualizada";
        String mensagem = criada
                ? "O professor publicou a atividade \"" + atividade.getNome() + "\"."
                : "O professor atualizou a atividade \"" + atividade.getNome() + "\".";
        String assunto = criada
                ? "Nova atividade - Learn Tech"
                : "Atividade atualizada - Learn Tech";

        Set<Integer> usuariosNotificados = new HashSet<>();
        for (Matricula matricula : matriculas.findByTurmaIdAndStatusIgnoreCase(turmaId, "ATIVA")) {
            Aluno aluno = matricula.getAluno();
            if (aluno == null || aluno.getUsuario() == null
                    || !usuariosNotificados.add(aluno.getUsuario().getId())) {
                continue;
            }

            create(aluno.getUsuario(), titulo, mensagem, criada ? "📚" : "✏️");
            enviarEmail(aluno.getEmail(), assunto, mensagem + "\n\nAcesse o Learn Tech para consultar os detalhes.");
        }
    }

    @Transactional
    public void notifyGrade(Aluno aluno, Atividade atividade, Double nota) {
        String titulo = "Atividade corrigida";
        String mensagem = "A atividade \"" + atividade.getNome()
                + "\" foi corrigida. Sua nota: " + nota + ".";
        create(aluno.getUsuario(), titulo, mensagem, "✅");
        enviarEmail(aluno.getEmail(), "Atividade corrigida - Learn Tech",
                mensagem + "\n\nAcesse o Learn Tech para consultar suas notas.");
    }

    @Transactional
    public void notifyDeadlineReminder(Aluno aluno, Atividade atividade, Priority prioridade, long diasRestantes) {
        String prazo = atividade.getPrazo().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String dias;
        if (diasRestantes < 0) {
            long diasAtrasados = Math.abs(diasRestantes);
            dias = "O prazo venceu há " + diasAtrasados
                    + (diasAtrasados == 1 ? " dia." : " dias.");
        } else if (diasRestantes == 0) {
            dias = "O prazo termina hoje.";
        } else {
            dias = "Faltam " + diasRestantes + (diasRestantes == 1 ? " dia." : " dias.");
        }
        String mensagem = "A atividade \"" + atividade.getNome() + "\" está com prioridade "
                + prioridade.name().toLowerCase(Locale.ROOT) + " e deve ser entregue até " + prazo + ". " + dias;
        create(aluno.getUsuario(), "Lembrete de entrega", mensagem, "⏰");
        enviarEmail(aluno.getEmail(), "Lembrete de entrega - Learn Tech",
                mensagem + "\n\nVocê receberá este lembrete diariamente até enviar a atividade.");
    }

    private Notificacao create(Usuario user, String titulo, String mensagem, String icone) {
        Notificacao n = new Notificacao();
        n.setUsuario(user);
        n.setTitulo(titulo);
        n.setMensagem(mensagem);
        n.setIcone(icone == null ? "🔔" : icone);
        return repository.save(n);
    }

    private void enviarEmail(String destinatario, String assunto, String mensagem) {
        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(fromAddress);
        email.setTo(destinatario);
        email.setSubject(assunto);
        email.setText(mensagem);
        try {
            mailSender.send(email);
        } catch (MailException erro) {
            logger.error("Falha ao enviar notificação por e-mail pelo SMTP {} para {}.",
                    mailHost, destinatario, erro);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A notificação foi preparada, mas não foi possível enviar o e-mail. Verifique a configuração SMTP.",
                    erro);
        }
    }

    @Transactional
    public void markRead(Integer usuarioId, Integer id) {
        Notificacao n = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notificação não encontrada"));
        if (!n.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notificação não pertence ao usuário");
        }
        n.setLida(true);
        repository.save(n);
    }

    @Transactional
    public void markAllRead(Integer usuarioId) {
        List<Notificacao> all = list(usuarioId);
        all.forEach(n -> n.setLida(true));
        repository.saveAll(all);
    }
}
