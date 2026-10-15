package com.learntech.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Turma")
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

    public Integer getId() { return id; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public String getSala() { return sala; }
    public void setSala(String sala) { this.sala = sala; }
    public String getLinkGrupo() { return linkGrupo; }
    public void setLinkGrupo(String linkGrupo) { this.linkGrupo = linkGrupo; }
    public Professor getProfessor() { return professor; }
    public void setProfessor(Professor professor) { this.professor = professor; }
}
