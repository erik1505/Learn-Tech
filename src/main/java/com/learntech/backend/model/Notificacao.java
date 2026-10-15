package com.learntech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Tabela NOVA (nao existe no LearnTech.sql original). Ver src/main/database/notificacao.sql */
@Entity
@Table(name = "Notificacao")
public class Notificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_notificacao")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idUsuario")
    private Usuario usuario;

    @Column(name = "Titulo", nullable = false, length = 160)
    private String titulo;

    @Column(name = "Mensagem", columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "Icone", length = 20)
    private String icone = "🔔";

    @Column(name = "Lida", nullable = false)
    private boolean lida = false;

    @Column(name = "Criada_em", nullable = false)
    private LocalDateTime criadaEm = LocalDateTime.now();

    public Integer getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
    public String getIcone() { return icone; }
    public void setIcone(String icone) { this.icone = icone; }
    public boolean isLida() { return lida; }
    public void setLida(boolean lida) { this.lida = lida; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
