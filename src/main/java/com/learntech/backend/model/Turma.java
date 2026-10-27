package com.learntech.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Turma", uniqueConstraints = @UniqueConstraint(
        name = "UK_Turma_Escola_Codigo", columnNames = {"EscolaId", "CodigoExterno"}))
public class Turma {
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
}
