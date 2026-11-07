package com.learntech.backend.model;

import jakarta.persistence.*;
import java.security.SecureRandom;
import java.util.Base64;

@Entity
@Table(name = "Turma", uniqueConstraints = @UniqueConstraint(
        name = "UK_Turma_Escola_Codigo", columnNames = {"EscolaId", "CodigoExterno"}),
        indexes = @Index(name = "UK_Turma_Codigo_convite", columnList = "Codigo_convite", unique = true))
public class Turma {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_turma")
    private Integer id;

    @Column(name = "Quantidade", nullable = false)
    private Integer quantidade;

    @Column(name = "Sala", nullable = false, length = 50)
    private String sala;

    @Column(name = "Link_grupo", nullable = false, length = 255)
    private String linkGrupo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "idProfessor")
    private Professor professor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EscolaId")
    private Escola escola;

    @Column(name = "CodigoExterno", length = 100)
    private String codigoExterno;

    @Column(name = "Codigo_convite", length = 64)
    private String codigoConvite;

    @PrePersist
    private void gerarCodigoConvite() {
        if (codigoConvite == null || codigoConvite.isBlank()) {
            byte[] token = new byte[24];
            RANDOM.nextBytes(token);
            codigoConvite = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        }
    }

    public void gerarCodigoConviteSeNecessario() {
        gerarCodigoConvite();
    }

    public Integer getId() { return id; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public String getLinkGrupo() { return linkGrupo; }
    public void setLinkGrupo(String linkGrupo) { this.linkGrupo = linkGrupo; }
    public Professor getProfessor() { return professor; }
    public void setProfessor(Professor professor) { this.professor = professor; }
    public Escola getEscola() { return escola; }
    public void setEscola(Escola escola) { this.escola = escola; }
    public String getCodigoExterno() { return codigoExterno; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
    public String getCodigoConvite() { return codigoConvite; }
    public void setCodigoConvite(String codigoConvite) { this.codigoConvite = codigoConvite; }
}
