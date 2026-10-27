package com.learntech.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "VinculoEscola", uniqueConstraints = {
        @UniqueConstraint(name = "UK_VinculoEscola_RoleCodigo",
                columnNames = {"EscolaId", "Role", "CodigoExterno"}),
        @UniqueConstraint(name = "UK_VinculoEscola_Usuario", columnNames = {"EscolaId", "UsuarioId"})
})
public class VinculoEscola {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_vinculo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "EscolaId", nullable = false)
    private Escola escola;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "UsuarioId", nullable = false)
    private Usuario usuario;

    @Column(name = "CodigoExterno", nullable = false, length = 100)
    private String codigoExterno;

    @Enumerated(EnumType.STRING)
    @Column(name = "Role", nullable = false, length = 20)
    private Role role;

    public Integer getId() { return id; }
    public Escola getEscola() { return escola; }
    public void setEscola(Escola escola) { this.escola = escola; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getCodigoExterno() { return codigoExterno; }
    public void setCodigoExterno(String codigoExterno) { this.codigoExterno = codigoExterno; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
