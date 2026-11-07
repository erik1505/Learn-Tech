package com.learntech.backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AlunoIdSchemaMigration implements ApplicationRunner {
    private final DataSource dataSource;

    public AlunoIdSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "Aluno")) {
                return;
            }

            if (!primaryKeyIsAlunoId(connection)) {
                migrate(connection);
            }
            backfillInvitationCodes(connection);
        } catch (SQLException error) {
            throw new IllegalStateException("Não foi possível migrar o esquema de alunos.", error);
        }
    }

    private void migrate(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = OFF");
        }
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE Aluno RENAME TO Aluno__legado");
            statement.execute("""
                    CREATE TABLE Aluno__novo (
                        Id_aluno INTEGER PRIMARY KEY AUTOINCREMENT,
                        RA VARCHAR(100) NOT NULL UNIQUE,
                        Nome VARCHAR(100) NOT NULL,
                        Email VARCHAR(100) NOT NULL,
                        Telefone VARCHAR(20) NOT NULL,
                        "idUsuário" INTEGER
                    )
                    """);
            statement.execute("""
                    INSERT INTO Aluno__novo (RA, Nome, Email, Telefone, "idUsuário")
                    SELECT CAST(RA AS TEXT), Nome, Email, Telefone, "idUsuário"
                    FROM Aluno__legado
                    """);

            rebuildMatriculas(statement);
            rebuildEntregas(statement);
            rebuildPostIts(statement);

            statement.execute("DROP TABLE Aluno__legado");
            statement.execute("ALTER TABLE Aluno__novo RENAME TO Aluno");
            statement.execute("ALTER TABLE Matricula__nova RENAME TO Matricula");
            statement.execute("ALTER TABLE Entrega_atividade__nova RENAME TO Entrega_atividade");
            statement.execute("ALTER TABLE \"Post-it__nova\" RENAME TO \"Post-it\"");
            try (ResultSet violations = statement.executeQuery("PRAGMA foreign_key_check")) {
                if (violations.next()) {
                    throw new SQLException("A migração encontrou relações de aluno sem correspondência.");
                }
            }
            connection.commit();
        } catch (SQLException error) {
            connection.rollback();
            throw error;
        } finally {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
        }
    }

    private void rebuildMatriculas(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Matricula__nova (
                    Id_matricula INTEGER PRIMARY KEY AUTOINCREMENT,
                    Status VARCHAR(50) NOT NULL,
                    Id_aluno INTEGER,
                    Id_turma INTEGER NOT NULL,
                    FOREIGN KEY (Id_aluno) REFERENCES Aluno__novo(Id_aluno),
                    FOREIGN KEY (Id_turma) REFERENCES Turma(Id_turma)
                )
                """);
        statement.execute("""
                INSERT INTO Matricula__nova (Id_matricula, Status, Id_aluno, Id_turma)
                SELECT m.Id_matricula, m.Status,
                       (SELECT a.Id_aluno FROM Aluno__novo a
                        WHERE a.RA = CAST(m.RA AS TEXT)),
                       m.Id_turma
                FROM Matricula m
                """);
        statement.execute("DROP TABLE Matricula");
    }

    private void rebuildEntregas(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE Entrega_atividade__nova (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    Id_atividade INTEGER,
                    Id_aluno INTEGER,
                    texto_comentario TEXT,
                    arquivo_url LONGTEXT,
                    Arquivo_enviado LONGTEXT,
                    Nota REAL,
                    prioridade VARCHAR(50),
                    Status_kanban VARCHAR(50),
                    Data_envio TIMESTAMP,
                    FOREIGN KEY (Id_aluno) REFERENCES Aluno__novo(Id_aluno),
                    FOREIGN KEY (Id_atividade) REFERENCES Atividades(Id_atividades)
                )
                """);
        statement.execute("""
                INSERT INTO Entrega_atividade__nova
                    (id, Id_atividade, Id_aluno, texto_comentario, arquivo_url,
                     Arquivo_enviado, Nota, prioridade, Status_kanban, Data_envio)
                SELECT e.id, e.Id_atividade,
                       (SELECT a.Id_aluno FROM Aluno__novo a
                        WHERE a.RA = CAST(e.RA AS TEXT)),
                       e.texto_comentario, e.arquivo_url, e.Arquivo_enviado,
                       e.Nota, e.prioridade, e.Status_kanban, e.Data_envio
                FROM Entrega_atividade e
                """);
        statement.execute("DROP TABLE Entrega_atividade");
    }

    private void rebuildPostIts(Statement statement) throws SQLException {
        statement.execute("""
                CREATE TABLE "Post-it__nova" (
                    Id_anotacao INTEGER PRIMARY KEY AUTOINCREMENT,
                    Titulo_post_it VARCHAR(100) NOT NULL,
                    Conteudo TEXT NOT NULL,
                    Id_atividade INTEGER NOT NULL,
                    Id_aluno INTEGER NOT NULL,
                    FOREIGN KEY (Id_atividade) REFERENCES Atividades(Id_atividades),
                    FOREIGN KEY (Id_aluno) REFERENCES Aluno__novo(Id_aluno)
                )
                """);
        statement.execute("""
                INSERT INTO "Post-it__nova"
                    (Id_anotacao, Titulo_post_it, Conteudo, Id_atividade, Id_aluno)
                SELECT p.Id_anotacao, p.Titulo_post_it, p.Conteudo, p.Id_atividade,
                       (SELECT a.Id_aluno FROM Aluno__novo a
                        WHERE a.RA = CAST(p.RA AS TEXT))
                FROM "Post-it" p
                """);
        statement.execute("DROP TABLE \"Post-it\"");
    }

    private void backfillInvitationCodes(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    UPDATE Turma
                    SET Codigo_convite = lower(hex(randomblob(24)))
                    WHERE Codigo_convite IS NULL OR trim(Codigo_convite) = ''
                    """);
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            query.setString(1, table);
            try (ResultSet result = query.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean primaryKeyIsAlunoId(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info('Aluno')")) {
            while (columns.next()) {
                if ("Id_aluno".equalsIgnoreCase(columns.getString("name"))) {
                    return columns.getInt("pk") > 0;
                }
            }
        }
        return false;
    }
}
