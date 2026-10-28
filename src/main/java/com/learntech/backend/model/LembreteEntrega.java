package com.learntech.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Lembrete_Entrega", uniqueConstraints = @UniqueConstraint(
        name = "UK_Lembrete_Entrega_Aluno_Dia",
        columnNames = {"Id_atividade", "RA", "Data_lembrete"}))
public class LembreteEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id_lembrete")
    private Integer id;

    @Column(name = "Id_atividade", nullable = false)
    private Integer atividadeId;

    @Column(name = "RA", nullable = false)
    private Integer alunoRa;

    @Column(name = "Data_lembrete", nullable = false)
    private LocalDate dataLembrete;

    public LembreteEntrega() {}

    public LembreteEntrega(Integer atividadeId, Integer alunoRa, LocalDate dataLembrete) {
        this.atividadeId = atividadeId;
        this.alunoRa = alunoRa;
        this.dataLembrete = dataLembrete;
    }
}
