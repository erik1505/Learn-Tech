package com.learntech.backend.service;

import com.learntech.backend.dto.AuthChallengeResponse;
import com.learntech.backend.dto.LoginChallengeResponse;
import com.learntech.backend.dto.LoginSessionResponse;
import com.learntech.backend.dto.ProfileUpdateRequest;
import com.learntech.backend.dto.RegisterRequest;
import com.learntech.backend.model.Aluno;
import com.learntech.backend.model.Atividade;
import com.learntech.backend.model.Disciplina;
import com.learntech.backend.model.EmailVerificationPurpose;
import com.learntech.backend.model.Professor;
import com.learntech.backend.model.Role;
import com.learntech.backend.model.Usuario;
import com.learntech.backend.repository.AlunoRepository;
import com.learntech.backend.repository.MatriculaRepository;
import com.learntech.backend.repository.ProfessorRepository;
import com.learntech.backend.repository.TurmaRepository;
import com.learntech.backend.repository.UsuarioRepository;
import com.learntech.backend.repository.VinculoEscolaRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class UserService {
    private final UsuarioRepository usuarios;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final PasswordEncoder encoder;
    private final EmailVerificationService emailVerification;
    private final LocalSmsGatewayService smsGateway;
    private final AuthSessionService authSessions;
    private final MatriculaRepository matriculas;
    private final TurmaRepository turmas;
    private final VinculoEscolaRepository vinculosEscola;

    public UserService(UsuarioRepository usuarios, ProfessorRepository professores,
                       AlunoRepository alunos, PasswordEncoder encoder,
                       EmailVerificationService emailVerification, LocalSmsGatewayService smsGateway,
                       AuthSessionService authSessions, MatriculaRepository matriculas, TurmaRepository turmas,
                       VinculoEscolaRepository vinculosEscola) {
        this.usuarios = usuarios;
        this.professores = professores;
        this.alunos = alunos;
        this.encoder = encoder;
        this.emailVerification = emailVerification;
        this.smsGateway = smsGateway;
        this.authSessions = authSessions;
        this.matriculas = matriculas;
        this.turmas = turmas;
        this.vinculosEscola = vinculosEscola;
    }

    private String normalizarTelefone(String telefone) {
        return telefone == null ? "" : telefone.replaceAll("\\D+", "");
    }

    private boolean telefoneBrasileiroValido(String telefone) {
        String digits = normalizarTelefone(telefone);
        if (digits.startsWith("55") && (digits.length() == 12 || digits.length() == 13)) {
            digits = digits.substring(2);
        }
        return digits.matches("[1-9][0-9]{9,10}");
    }

    private String mascararEmail(String email) {
        int separador = email.indexOf('@');
        return email.substring(0, 1) + "***" + email.substring(separador);
    }

    private String mascararTelefone(String telefone) {
        String digits = normalizarTelefone(telefone);
        return "Final " + digits.substring(digits.length() - 4);
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

    public LoginChallengeResponse iniciarLogin(String email, String senha) {
        Usuario usuario = usuarios.findByLoginIgnoreCase(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos"));
        if (!encoder.matches(senha, usuario.getSenha())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        }

        UserProfile perfil = profileOf(usuario.getId());
        boolean smsDisponivel = smsGateway.configurado() && telefoneBrasileiroValido(perfil.telefone());
        return new LoginChallengeResponse(
                emailVerification.prepararDesafioLogin(usuario.getLogin()),
                mascararEmail(usuario.getLogin()),
                smsDisponivel ? mascararTelefone(perfil.telefone()) : null,
                smsDisponivel,
                "Senha confirmada. Escolha onde deseja receber seu código."
        );
    }

    public AuthChallengeResponse enviarCodigoLogin(String challengeId, String canal) {
        String telefone = null;
        if ("SMS".equals(canal)) {
            String login = emailVerification.loginDoDesafioLogin(challengeId);
            Usuario usuario = usuarios.findByLoginIgnoreCase(login)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Desafio inválido ou expirado."));
            telefone = profileOf(usuario.getId()).telefone();
            if (!smsGateway.configurado()) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "O gateway local de SMS não está configurado no servidor.");
            }
            if (!telefoneBrasileiroValido(telefone)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Não há um telefone válido com DDD cadastrado.");
            }
        }
        return emailVerification.enviarCodigoLogin(challengeId, canal, telefone);
    }

    public LoginSessionResponse verificarCodigoLogin(String challengeId, String codigo) {
        String login = emailVerification.verificarCodigo(challengeId, codigo, EmailVerificationPurpose.LOGIN)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado."));
        Usuario usuario = usuarios.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado."));
        UserProfile profile = profileOf(usuario.getId());
        return LoginSessionResponse.of(authSessions.create(usuario.getId()), profile);
    }

    public void logout(String token) {
        authSessions.revoke(token);
    }

    @Transactional
    public AuthChallengeResponse solicitarRedefinicaoSenha(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        return usuarios.findByLoginIgnoreCase(normalizedEmail)
                .map(usuario -> emailVerification.enviarCodigo(
                        usuario.getLogin(),
                        EmailVerificationPurpose.PASSWORD_RESET
                ))
                .orElseGet(() -> new AuthChallengeResponse(
                        java.util.UUID.randomUUID().toString(),
                        "Se o e-mail estiver cadastrado, você receberá um código para redefinir a senha."
                ));
    }

    @Transactional
    public void redefinirSenha(String challengeId, String codigo, String novaSenha) {
        String login = emailVerification.verificarCodigo(
                        challengeId,
                        codigo,
                        EmailVerificationPurpose.PASSWORD_RESET
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado."));
        Usuario usuario = usuarios.findByLoginIgnoreCase(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Código inválido ou expirado."));
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
        Professor professor = professores.findById(professorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente professor pode realizar esta ação"));
        AuthenticatedUser current = currentUser();
        if (current.role() != Role.PROFESSOR || !current.perfilId().equals(professor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação não autorizada para este professor");
        }
        return professor;
    }

    public Aluno requireAluno(Integer alunoId) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente aluno pode realizar esta ação"));
        AuthenticatedUser current = currentUser();
        if (current.role() != Role.ALUNO || !current.perfilId().equals(aluno.getRa())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação não autorizada para este aluno");
        }
        return aluno;
    }

    public void requireCurrentUser(Integer userId) {
        if (!currentUser().usuarioId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ação não autorizada para este usuário");
        }
    }

    public UserProfile currentProfile() {
        return profileOf(currentUser().usuarioId());
    }

    public List<Integer> currentSchoolIds() {
        return vinculosEscola.findByUsuarioId(currentUser().usuarioId()).stream()
                .map(vinculo -> vinculo.getEscola().getId())
                .distinct()
                .toList();
    }

    public boolean belongsToSchool(Integer schoolId) {
        return currentSchoolIds().contains(schoolId);
    }

    public void requireTurmaAccess(Integer turmaId, boolean professorOnly) {
        AuthenticatedUser current = currentUser();
        var turma = turmas.findById(turmaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Turma não encontrada"));
        if (current.role() == Role.PROFESSOR) {
            if (!turma.getProfessor().getId().equals(current.perfilId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Turma não pertence a este professor");
            }
            return;
        }
        if (professorOnly || current.role() != Role.ALUNO
                || !matriculas.existsByAlunoRaAndTurmaIdAndStatusIgnoreCase(current.perfilId(), turmaId, "ATIVA")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso não autorizado a esta turma");
        }
    }

    public void requireActivityAccess(Atividade atividade, boolean professorOnly) {
        if (atividade.getDisciplina() == null || atividade.getDisciplina().getTurma() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Atividade sem turma associada");
        }
        requireTurmaAccess(atividade.getDisciplina().getTurma().getId(), professorOnly);
    }

    public void requireStudentEnrollmentForCurrentTeacher(Integer turmaId, Integer alunoRa) {
        requireTurmaAccess(turmaId, true);
        if (!matriculas.existsByAlunoRaAndTurmaIdAndStatusIgnoreCase(alunoRa, turmaId, "ATIVA")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "O aluno não está matriculado nesta turma");
        }
    }

    public void requireDisciplinaAccess(Disciplina disciplina) {
        if (disciplina.getTurma() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Disciplina sem turma associada");
        }
        requireTurmaAccess(disciplina.getTurma().getId(), false);
    }

    private AuthenticatedUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória");
    }
}