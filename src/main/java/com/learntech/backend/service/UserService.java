package com.learntech.backend.service;

import com.learntech.backend.dto.ProfileUpdateRequest;
import com.learntech.backend.dto.RegisterRequest;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Professor;
import com.learntech.backend.model.Role;
import com.learntech.backend.model.Usuario;
import com.learntech.backend.repository.AlunoRepository;
import com.learntech.backend.repository.ProfessorRepository;
import com.learntech.backend.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UsuarioRepository usuarios;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final PasswordEncoder encoder;

    public UserService(UsuarioRepository usuarios, ProfessorRepository professores,
                       AlunoRepository alunos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.professores = professores;
        this.alunos = alunos;
        this.encoder = encoder;
    }

    @Transactional
    public UserProfile register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (usuarios.existsByLoginIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        Usuario usuario = new Usuario();
        usuario.setLogin(email);
        usuario.setSenha(encoder.encode(request.senha()));
        usuario = usuarios.save(usuario);

        if (request.role() == Role.PROFESSOR) {
            Professor p = new Professor();
            p.setNome(request.nome());
            p.setEmail(email);
            p.setTelefone(request.telefone());
            p.setUsuario(usuario);
            professores.save(p);
        } else {
            Aluno a = new Aluno();
            a.setNome(request.nome());
            a.setEmail(email);
            a.setTelefone(request.telefone());
            a.setUsuario(usuario);
            alunos.save(a);
        }
        return profileOf(usuario.getId());
    }

    public UserProfile login(String email, String senha) {
        Usuario usuario = usuarios.findByLoginIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));
        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }
        return profileOf(usuario.getId());
    }

    @Transactional
    public void redefinirSenha(String email, String novaSenha) {
        Usuario usuario = usuarios.findByLoginIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "E-mail não encontrado"));

        usuario.setSenha(encoder.encode(novaSenha));
        usuarios.save(usuario);
    }

    public Usuario get(Integer usuarioId) {
        return usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));
    }

    public UserProfile profileOf(Integer usuarioId) {
        var prof = professores.findByUsuarioId(usuarioId);
        if (prof.isPresent()) {
            Professor p = prof.get();
            return new UserProfile(usuarioId, p.getId(), p.getNome(), p.getEmail(), p.getTelefone(), Role.PROFESSOR);
        }
        Aluno a = alunos.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));
        return new UserProfile(usuarioId, a.getRa(), a.getNome(), a.getEmail(), a.getTelefone(), Role.ALUNO);
    }

    @Transactional
    public UserProfile update(Integer usuarioId, ProfileUpdateRequest request) {
        Usuario usuario = get(usuarioId);
        String email = request.email().trim().toLowerCase();
        if (usuarios.findByLoginIgnoreCase(email).filter(u -> !u.getId().equals(usuarioId)).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        usuario.setLogin(email);
        usuarios.save(usuario);

        var prof = professores.findByUsuarioId(usuarioId);
        if (prof.isPresent()) {
            Professor p = prof.get();
            p.setNome(request.nome());
            p.setEmail(email);
            professores.save(p);
        } else {
            Aluno a = alunos.findByUsuarioId(usuarioId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Perfil não encontrado"));
            a.setNome(request.nome());
            a.setEmail(email);
            alunos.save(a);
        }
        return profileOf(usuarioId);
    }

    public Professor requireProfessor(Integer professorId) {
        return professores.findById(professorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente professor pode realizar esta ação"));
    }

    public Aluno requireAluno(Integer alunoId) {
        return alunos.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente aluno pode realizar esta ação"));
    }
}