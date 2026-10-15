DROP DATABASE IF EXISTS Learn_tech;
CREATE DATABASE Learn_tech CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE Learn_tech;

CREATE TABLE `Usuário` (
    ID_usuario INT PRIMARY KEY AUTO_INCREMENT,
    Login VARCHAR(100) NOT NULL UNIQUE,
    Senha VARCHAR(255) NOT NULL
);

CREATE TABLE Professor (
    ID_PROFESSOR INT PRIMARY KEY AUTO_INCREMENT,
    Nome VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    Telefone VARCHAR(20) NOT NULL,
    `idUsuário` INT,
    FOREIGN KEY (`idUsuário`) REFERENCES `Usuário`(ID_usuario) ON DELETE CASCADE
);

CREATE TABLE Turma (
    Id_turma INT PRIMARY KEY AUTO_INCREMENT,
    Quantidade INT NOT NULL,
    Sala VARCHAR(50) NOT NULL,
    Link_grupo VARCHAR(255) NOT NULL,
    idProfessor INT NOT NULL,
    FOREIGN KEY (idProfessor) REFERENCES Professor(ID_PROFESSOR)
);

CREATE TABLE Aluno (
    RA INT PRIMARY KEY AUTO_INCREMENT,
    Nome VARCHAR(100) NOT NULL,
    Email VARCHAR(100) NOT NULL,
    Telefone VARCHAR(20) NOT NULL,
    `idUsuário` INT,
    FOREIGN KEY (`idUsuário`) REFERENCES `Usuário`(ID_usuario) ON DELETE CASCADE
);

CREATE TABLE Disciplina (
    ID_Disciplina INT PRIMARY KEY AUTO_INCREMENT,
    Nome_disciplina VARCHAR(100),
    Status VARCHAR(50) NOT NULL,
    idTurma INT NOT NULL,
    FOREIGN KEY (idTurma) REFERENCES Turma(Id_turma)
);

CREATE TABLE Atividades (
    Id_atividades INT PRIMARY KEY AUTO_INCREMENT,
    Nome_atividade VARCHAR(100) NOT NULL,
    Descricao_atividade TEXT NOT NULL,
    Prazo DATETIME NOT NULL,
    Status VARCHAR(50) NOT NULL,
    idDisciplina INT NOT NULL,
    FOREIGN KEY (idDisciplina) REFERENCES Disciplina(ID_Disciplina)
);

CREATE TABLE Matricula (
    Id_matricula INT PRIMARY KEY AUTO_INCREMENT,
    Status VARCHAR(50) NOT NULL,
    RA INT,
    Id_turma INT NOT NULL,
    FOREIGN KEY (RA) REFERENCES Aluno(RA),
    FOREIGN KEY (Id_turma) REFERENCES Turma(Id_turma)
);

CREATE TABLE Entrega_atividade (
    id INT PRIMARY KEY AUTO_INCREMENT,
    prioridade VARCHAR(50) DEFAULT 'MEDIA',
    Status_kanban VARCHAR(50) DEFAULT 'A_FAZER',
    Nota DOUBLE,
    texto_comentario TEXT,
    arquivo_url LONGTEXT,
    Arquivo_enviado LONGTEXT,
    Data_envio DATETIME NOT NULL,
    RA INT NOT NULL,
    Id_atividade INT NOT NULL,
    FOREIGN KEY (RA) REFERENCES Aluno(RA),
    FOREIGN KEY (Id_atividade) REFERENCES Atividades(Id_atividades)
);

CREATE TABLE `Post-it` (
    Id_anotacao INT PRIMARY KEY AUTO_INCREMENT,
    Titulo_post_it VARCHAR(100) NOT NULL,
    Conteudo TEXT NOT NULL,
    Id_atividade INT NOT NULL,
    RA INT NOT NULL,
    FOREIGN KEY (Id_atividade) REFERENCES Atividades(Id_atividades),
    FOREIGN KEY (RA) REFERENCES Aluno(RA)
);

CREATE TABLE Notificacao (
    Id_notificacao INT PRIMARY KEY AUTO_INCREMENT,
    Titulo VARCHAR(160) NOT NULL,
    Mensagem TEXT,
    Icone VARCHAR(20),
    Lida BIT(1) NOT NULL DEFAULT 0,
    Criada_em DATETIME NOT NULL,
    idUsuario INT NOT NULL,
    FOREIGN KEY (idUsuario) REFERENCES `Usuário`(ID_usuario) ON DELETE CASCADE
) DEFAULT CHARSET = utf8mb4;