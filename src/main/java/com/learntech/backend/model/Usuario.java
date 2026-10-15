package com.learntech.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "`Usuário`")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_usuario")
    private Integer id;

    @Column(name = "Login", nullable = false, length = 100)
    private String login;

    @Column(name = "Senha", nullable = false, length = 100)
    private String senha;

    public Integer getId() { return id; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
}
