package com.learntech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Entrega_atividade")
public class EntregaAtividade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "Id_atividade")
    private Atividade atividade;

    @ManyToOne
    @JoinColumn(name = "RA")
    private Aluno aluno;

    @Column(name = "texto_comentario", columnDefinition = "TEXT")
    private String textoComentario;

    @Column(name = "arquivo_url", columnDefinition = "LONGTEXT")
    private String arquivoUrl;

    @Column(name = "Arquivo_enviado", columnDefinition = "LONGTEXT")
    private String arquivoEnviado;

    @Column(name = "Nota")
    private Double nota;

    @Column(name = "prioridade")
    private String prioridade = "MEDIA";

    @Column(name = "Status_kanban")
    private String statusKanban = "A_FAZER";

    @Column(name = "Data_envio")
    private LocalDateTime dataEnvio;

    public EntregaAtividade() {}

    // Getters e Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Atividade getAtividade() { return atividade; }
    public void setAtividade(Atividade atividade) { this.atividade = atividade; }

    public Aluno getAluno() { return aluno; }
    public void setAluno(Aluno aluno) { this.aluno = aluno; }

    public String getTextoComentario() { return textoComentario; }
    public void setTextoComentario(String textoComentario) { this.textoComentario = textoComentario; }

    public String getArquivoUrl() { return arquivoUrl; }
    public void setArquivoUrl(String arquivoUrl) { this.arquivoUrl = arquivoUrl; }

    public String getArquivoEnviado() {
        return arquivoEnviado != null ? arquivoEnviado : arquivoUrl;
    }
    public void setArquivoEnviado(String arquivoEnviado) {
        this.arquivoEnviado = arquivoEnviado;
        this.arquivoUrl = arquivoEnviado;
    }

    public Double getNota() { return nota; }
    public void setNota(Double nota) { this.nota = nota; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }

    public String getStatusKanban() { return statusKanban; }
    public void setStatusKanban(String statusKanban) { this.statusKanban = statusKanban; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public boolean foiEnviada() {
        if ("CONCLUIDA".equalsIgnoreCase(statusKanban)) {
            return true;
        }
        return "A_FAZER".equalsIgnoreCase(statusKanban)
                && ((textoComentario != null && !textoComentario.isBlank())
                || (getArquivoEnviado() != null && !getArquivoEnviado().isBlank()));
    }
}