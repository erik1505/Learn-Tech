package com.learntech.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "Escola")
public class Escola {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_escola")
    private Integer id;

    @Column(name = "Codigo_externo", nullable = false, unique = true, length = 100)
    private String codigoExterno;

    @Column(name = "Nome", nullable = false, length = 160)
    private String nome;

    public Integer getId() { return id; }
    public String getCodigoExterno() { return codigoExterno; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
}
