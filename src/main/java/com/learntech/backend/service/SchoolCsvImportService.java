package com.learntech.backend.service;

import com.learntech.backend.model.*;
import com.learntech.backend.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class SchoolCsvImportService {
    private static final int MAX_FILE_BYTES = 10 * 1024 * 1024;
    private static final int MAX_ROWS = 10_000;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> HEADERS = List.of(
            "codigo_escola", "nome_escola", "codigo_turma", "nome_turma", "disciplina",
            "codigo_professor", "nome_professor", "email_professor", "telefone_professor",
            "codigo_aluno", "nome_aluno", "email_aluno", "telefone_aluno"
    );
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE = Pattern.compile("[1-9][0-9]{9,10}");

    private final EscolaRepository escolas;
    private final VinculoEscolaRepository vinculos;
    private final TurmaRepository turmas;
    private final ProfessorRepository professores;
    private final AlunoRepository alunos;
    private final UsuarioRepository usuarios;
    private final DisciplinaRepository disciplinas;
    private final MatriculaRepository matriculas;
    private final PasswordEncoder passwordEncoder;

    public SchoolCsvImportService(EscolaRepository escolas, VinculoEscolaRepository vinculos,
                                  TurmaRepository turmas, ProfessorRepository professores,
                                  AlunoRepository alunos, UsuarioRepository usuarios,
                                  DisciplinaRepository disciplinas, MatriculaRepository matriculas,
                                  PasswordEncoder passwordEncoder) {
        this.escolas = escolas;
        this.vinculos = vinculos;
        this.turmas = turmas;
        this.professores = professores;
        this.alunos = alunos;
        this.usuarios = usuarios;
        this.disciplinas = disciplinas;
        this.matriculas = matriculas;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ImportReport importCsv(Path file, boolean dryRun) {
        List<Row> rows = readAndValidate(file);
        validateAgainstExistingAccounts(rows);
        if (dryRun) {
            long newAccounts = rows.stream().flatMap(row -> List.of(
                            new Person(row.teacherCode(), row.teacherName(), row.teacherEmail(), row.teacherPhone(), Role.PROFESSOR),
                            new Person(row.studentCode(), row.studentName(), row.studentEmail(), row.studentPhone(), Role.ALUNO)
                    ).stream())
                    .map(Person::email).distinct()
                    .filter(email -> usuarios.findByLoginIgnoreCase(email).isEmpty())
                    .count();
            return new ImportReport(true, rows.size(), distinctSchoolCount(rows), distinctClassCount(rows), newAccounts, 0);
        }

        Map<String, Escola> schools = new HashMap<>();
        Map<String, PersonResult> people = new HashMap<>();
        Map<String, Turma> classes = new HashMap<>();
        int createdAccounts = 0;
        int createdEnrollments = 0;

        for (Row row : rows) {
            Escola school = schools.computeIfAbsent(row.schoolCode(), ignored -> escolas
                    .findByCodigoExterno(row.schoolCode()).orElseGet(Escola::new));
            school.setCodigoExterno(row.schoolCode());
            school.setNome(row.schoolName());
            school = escolas.save(school);
            schools.put(row.schoolCode(), school);
            Escola activeSchool = school;

            String teacherKey = row.schoolCode() + "\u0000PROFESSOR\u0000" + row.teacherCode();
            PersonResult teacher = people.get(teacherKey);
            if (teacher == null) {
                teacher = resolveUser(activeSchool, new Person(row.teacherCode(), row.teacherName(),
                        row.teacherEmail(), row.teacherPhone(), Role.PROFESSOR));
                people.put(teacherKey, teacher);
                if (teacher.created()) createdAccounts++;
            }

            String studentKey = row.schoolCode() + "\u0000ALUNO\u0000" + row.studentCode();
            PersonResult student = people.get(studentKey);
            if (student == null) {
                student = resolveUser(activeSchool, new Person(row.studentCode(), row.studentName(),
                        row.studentEmail(), row.studentPhone(), Role.ALUNO));
                people.put(studentKey, student);
                if (student.created()) createdAccounts++;
            }

            String classKey = row.schoolCode() + "\u0000" + row.classCode();
            Turma turma = classes.computeIfAbsent(classKey, ignored -> turmas
                    .findByEscolaIdAndCodigoExterno(activeSchool.getId(), row.classCode()).orElseGet(Turma::new));
            turma.setEscola(activeSchool);
            turma.setCodigoExterno(row.classCode());
            turma.setSala(row.className() + " - " + row.subject());
            turma.setProfessor((Professor) teacher.profile());
            turma.setLinkGrupo(turma.getLinkGrupo() == null ? "" : turma.getLinkGrupo());
            turma.setQuantidade(0);
            turma = turmas.save(turma);
            classes.put(classKey, turma);

            Disciplina disciplina = disciplinas.findFirstByTurmaIdAndNome(turma.getId(), row.subject())
                    .orElseGet(Disciplina::new);
            disciplina.setNome(row.subject());
            disciplina.setStatus("ATIVA");
            disciplina.setTurma(turma);
            disciplinas.save(disciplina);

            Aluno aluno = (Aluno) student.profile();
            if (!matriculas.existsByAlunoRaAndTurmaId(aluno.getRa(), turma.getId())) {
                Matricula enrollment = new Matricula();
                enrollment.setAluno(aluno);
                enrollment.setTurma(turma);
                enrollment.setStatus("ATIVA");
                matriculas.save(enrollment);
                createdEnrollments++;
            }
        }

        for (Turma turma : classes.values()) {
            turma.setQuantidade((int) matriculas.findByTurmaIdAndStatusIgnoreCase(turma.getId(), "ATIVA").stream()
                    .map(Matricula::getAluno)
                    .filter(Objects::nonNull)
                    .map(Aluno::getRa)
                    .distinct()
                    .count());
            turmas.save(turma);
        }
        return new ImportReport(false, rows.size(), schools.size(), classes.size(), createdAccounts, createdEnrollments);
    }

    private PersonResult resolveUser(Escola school, Person person) {
        VinculoEscola link = vinculos.findByEscolaIdAndCodigoExternoAndRole(
                        school.getId(), person.externalCode(), person.role())
                .orElse(null);
        Usuario user;
        Object profile;
        boolean created = false;
        if (link != null) {
            user = link.getUsuario();
            profile = loadProfile(user.getId(), person.role());
            var emailOwner = usuarios.findByLoginIgnoreCase(person.email()).orElse(null);
            if (emailOwner != null && !emailOwner.getId().equals(user.getId())) {
                throw conflict("O e-mail " + person.email() + " já pertence a outra conta.");
            }
        } else {
            user = usuarios.findByLoginIgnoreCase(person.email()).orElse(null);
            if (user == null) {
                user = new Usuario();
                user.setLogin(person.email());
                user.setSenha(passwordEncoder.encode(randomPassword()));
                user = usuarios.save(user);
                profile = createProfile(user, person);
                created = true;
            } else {
                profile = loadProfile(user.getId(), person.role());
            }

            VinculoEscola existingUserLink = vinculos.findByEscolaIdAndUsuarioId(school.getId(), user.getId())
                    .orElse(null);
            if (existingUserLink == null) {
                existingUserLink = new VinculoEscola();
                existingUserLink.setEscola(school);
                existingUserLink.setUsuario(user);
                existingUserLink.setRole(person.role());
            } else if (existingUserLink.getRole() != person.role()) {
                throw conflict("O e-mail " + person.email() + " já está vinculado a outro perfil nesta escola.");
            }
            existingUserLink.setCodigoExterno(person.externalCode());
            vinculos.save(existingUserLink);
        }

        user.setLogin(person.email());
        usuarios.save(user);
        updateProfile(profile, person);
        return new PersonResult(profile, created);
    }

    private Object createProfile(Usuario user, Person person) {
        if (person.role() == Role.PROFESSOR) {
            Professor professor = new Professor();
            professor.setUsuario(user);
            professor.setNome(person.name());
            professor.setEmail(person.email());
            professor.setTelefone(person.phone());
            return professores.save(professor);
        }
        Aluno aluno = new Aluno();
        aluno.setUsuario(user);
        aluno.setNome(person.name());
        aluno.setEmail(person.email());
        aluno.setTelefone(person.phone());
        return alunos.save(aluno);
    }

    private Object loadProfile(Integer userId, Role role) {
        if (role == Role.PROFESSOR) {
            return professores.findByUsuarioId(userId)
                    .orElseThrow(() -> conflict("A conta vinculada não possui perfil de professor."));
        }
        return alunos.findByUsuarioId(userId)
                .orElseThrow(() -> conflict("A conta vinculada não possui perfil de aluno."));
    }

    private void updateProfile(Object profile, Person person) {
        if (profile instanceof Professor professor) {
            professor.setNome(person.name());
            professor.setEmail(person.email());
            professor.setTelefone(person.phone());
            professores.save(professor);
        } else if (profile instanceof Aluno aluno) {
            aluno.setNome(person.name());
            aluno.setEmail(person.email());
            aluno.setTelefone(person.phone());
            alunos.save(aluno);
        }
    }

    private void validateAgainstExistingAccounts(List<Row> rows) {
        Map<String, Role> roles = new HashMap<>();
        for (Row row : rows) {
            for (Person person : List.of(
                    new Person(row.teacherCode(), row.teacherName(), row.teacherEmail(), row.teacherPhone(), Role.PROFESSOR),
                    new Person(row.studentCode(), row.studentName(), row.studentEmail(), row.studentPhone(), Role.ALUNO))) {
                Role previousRole = roles.putIfAbsent(person.email(), person.role());
                if (previousRole != null && previousRole != person.role()) {
                    throw conflict("O mesmo e-mail aparece como aluno e professor: " + person.email());
                }
                Usuario existing = usuarios.findByLoginIgnoreCase(person.email()).orElse(null);
                if (existing != null) {
                    loadProfile(existing.getId(), person.role());
                }
            }
        }
    }

    private List<Row> readAndValidate(Path file) {
        String content;
        try {
            long size = Files.size(file);
            if (size == 0 || size > MAX_FILE_BYTES) {
                throw badRequest("O CSV deve ter conteúdo e não pode ultrapassar 10 MB.");
            }
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalArgumentException("Não foi possível ler o arquivo CSV informado.", error);
        }
        if (content.startsWith("\uFEFF")) content = content.substring(1);
        List<List<String>> records = parseCsv(content);
        if (records.size() < 2) throw badRequest("O CSV precisa conter o cabeçalho e pelo menos uma linha de dados.");
        if (records.size() - 1 > MAX_ROWS) throw badRequest("O CSV pode conter no máximo 10.000 linhas.");
        List<String> headers = records.get(0).stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).toList();
        if (!headers.equals(HEADERS)) {
            throw badRequest("Cabeçalho inválido. Use exatamente o modelo importacao-escola.csv.");
        }

        List<Row> rows = new ArrayList<>();
        for (int index = 1; index < records.size(); index++) {
            List<String> fields = records.get(index).stream().map(String::trim).toList();
            int line = index + 1;
            if (fields.size() != HEADERS.size()) {
                throw badRequest("Linha " + line + ": esperados 13 campos, encontrados " + fields.size() + ".");
            }
            if (fields.stream().anyMatch(String::isBlank)) {
                throw badRequest("Linha " + line + ": todos os campos são obrigatórios.");
            }
            Row row = new Row(fields, line);
            validateRow(row);
            rows.add(row);
        }
        validateConsistency(rows);
        return rows;
    }

    private void validateRow(Row row) {
        if (!EMAIL.matcher(row.teacherEmail()).matches() || row.teacherEmail().length() > 100) {
            throw badRequest("Linha " + row.line() + ": e-mail do professor inválido.");
        }
        if (!EMAIL.matcher(row.studentEmail()).matches() || row.studentEmail().length() > 100) {
            throw badRequest("Linha " + row.line() + ": e-mail do aluno inválido.");
        }
        if (!PHONE.matcher(row.teacherPhone()).matches() || !PHONE.matcher(row.studentPhone()).matches()) {
            throw badRequest("Linha " + row.line() + ": telefone deve conter DDD e 10 ou 11 dígitos, somente números.");
        }
        if (row.schoolCode().length() > 100 || row.classCode().length() > 100
                || row.teacherCode().length() > 100 || row.studentCode().length() > 100) {
            throw badRequest("Linha " + row.line() + ": códigos externos podem ter no máximo 100 caracteres.");
        }
        if (row.schoolName().length() > 160 || row.teacherName().length() > 100
                || row.studentName().length() > 100 || row.className().length() + row.subject().length() + 3 > 50
                || row.subject().length() > 100) {
            throw badRequest("Linha " + row.line() + ": um nome excede o limite aceito pelo sistema.");
        }
    }

    private void validateConsistency(List<Row> rows) {
        Map<String, String> schoolNames = new HashMap<>();
        Map<String, ClassIdentity> classesByCode = new HashMap<>();
        Map<String, PersonIdentity> peopleByCode = new HashMap<>();
        Map<String, PersonIdentity> peopleByEmail = new HashMap<>();
        for (Row row : rows) {
            putConsistent(schoolNames, row.schoolCode(), row.schoolName(), row, "nome da escola");
            putConsistent(classesByCode, row.schoolCode() + "\u0000" + row.classCode(),
                    new ClassIdentity(row.className(), row.subject(), row.teacherCode()), row,
                    "nome da turma, disciplina ou professor");
            putConsistent(peopleByCode, row.schoolCode() + "\u0000PROFESSOR\u0000" + row.teacherCode(),
                    new PersonIdentity(row.teacherName(), row.teacherEmail(), row.teacherPhone(), Role.PROFESSOR),
                    row, "dados do professor");
            putConsistent(peopleByCode, row.schoolCode() + "\u0000ALUNO\u0000" + row.studentCode(),
                    new PersonIdentity(row.studentName(), row.studentEmail(), row.studentPhone(), Role.ALUNO),
                    row, "dados do aluno");
            putConsistent(peopleByEmail, row.teacherEmail().toLowerCase(Locale.ROOT),
                    new PersonIdentity(row.teacherName(), row.teacherEmail().toLowerCase(Locale.ROOT),
                            row.teacherPhone(), Role.PROFESSOR),
                    row, "dados do e-mail");
            putConsistent(peopleByEmail, row.studentEmail().toLowerCase(Locale.ROOT),
                    new PersonIdentity(row.studentName(), row.studentEmail().toLowerCase(Locale.ROOT),
                            row.studentPhone(), Role.ALUNO),
                    row, "dados do e-mail");
        }
    }

    private <T> void putConsistent(Map<String, T> indexed, String key, T value, Row row, String description) {
        T previous = indexed.putIfAbsent(key, value);
        if (previous != null && !previous.equals(value)) {
            throw badRequest("Linha " + row.line() + ": " + description + " inconsistente para o mesmo código.");
        }
    }

    private List<List<String>> parseCsv(String text) {
        char delimiter = text.lines().findFirst().orElse("").chars().filter(c -> c == ',' || c == ';')
                .mapToObj(c -> (char) c)
                .collect(java.util.stream.Collectors.groupingBy(c -> c, java.util.stream.Collectors.counting()))
                .entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(',');
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < text.length() && text.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (!quoted && current == delimiter) {
                row.add(field.toString());
                field.setLength(0);
            } else if (!quoted && (current == '\n' || current == '\r')) {
                if (current == '\r' && index + 1 < text.length() && text.charAt(index + 1) == '\n') index++;
                row.add(field.toString());
                field.setLength(0);
                if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
                row = new ArrayList<>();
            } else {
                field.append(current);
            }
        }
        if (quoted) throw badRequest("CSV inválido: há um campo com aspas sem fechamento.");
        row.add(field.toString());
        if (row.stream().anyMatch(value -> !value.isBlank())) rows.add(row);
        return rows;
    }

    private int distinctSchoolCount(List<Row> rows) {
        return (int) rows.stream().map(Row::schoolCode).distinct().count();
    }

    private int distinctClassCount(List<Row> rows) {
        return (int) rows.stream().map(row -> row.schoolCode() + "\u0000" + row.classCode()).distinct().count();
    }

    private String randomPassword() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private record Person(String externalCode, String name, String email, String phone, Role role) {}
    private record PersonIdentity(String name, String email, String phone, Role role) {}
    private record ClassIdentity(String name, String subject, String teacherCode) {}
    private record PersonResult(Object profile, boolean created) {}
    private record Row(String schoolCode, String schoolName, String classCode, String className, String subject,
                       String teacherCode, String teacherName, String teacherEmail, String teacherPhone,
                       String studentCode, String studentName, String studentEmail, String studentPhone, int line) {
        Row(List<String> fields, int line) {
            this(fields.get(0), fields.get(1), fields.get(2), fields.get(3), fields.get(4),
                    fields.get(5), fields.get(6), fields.get(7).toLowerCase(Locale.ROOT), fields.get(8),
                    fields.get(9), fields.get(10), fields.get(11).toLowerCase(Locale.ROOT), fields.get(12), line);
        }
    }
    public record ImportReport(boolean dryRun, int rows, int schools, int classes, long createdAccounts,
                               int createdEnrollments) {}
}
