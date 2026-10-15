package com.learntech.backend.service;

import com.learntech.backend.model.Turma;
import com.learntech.backend.repository.TurmaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TurmaService {

    private final TurmaRepository turmaRepository;

    public TurmaService(TurmaRepository turmaRepository) {
        this.turmaRepository = turmaRepository;
    }

    public List<Turma> listarTodas() {
        return turmaRepository.findAll();
    }

    public List<Turma> listarPorProfessor(Integer professorId) {
        return turmaRepository.findByProfessorId(professorId);
    }

    public List<Turma> listarPorAluno(Integer alunoId) {
        return turmaRepository.findByAlunoId(alunoId);
    }
}