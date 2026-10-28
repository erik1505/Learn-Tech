# Importação escolar interna

A importação é executada por um operador autorizado da Learn Tech no ambiente do backend. Não existe endpoint nem tela pública para enviar CSV. A escola deve transferir o arquivo por um canal aprovado pela Learn Tech (por exemplo, armazenamento/SFTP com acesso restrito); não envie planilhas com dados de estudantes por e-mail comum.

## Preparar o arquivo

Use [modelo-importacao-escola.csv](../modelos-csv/modelo-importacao-escola.csv), codificado em UTF-8. O modelo usa ponto e vírgula como separador para que, ao abrir no Excel em português, cada campo apareça em uma coluna. O importador também aceita arquivos separados por vírgula. Cada linha representa a matrícula de um aluno em uma turma/disciplina e repete os dados da escola, turma e professor.

| Coluna | Obrigatória | Significado |
|---|---|---|
| `codigo_escola` | Sim | Código estável da escola no sistema de origem |
| `nome_escola` | Sim | Nome da escola |
| `codigo_turma` | Sim | Código estável para a combinação turma/disciplina |
| `nome_turma` | Sim | Identificação da sala/turma, por exemplo `1B` |
| `disciplina` | Sim | Matéria, por exemplo `Matemática` |
| `codigo_professor` | Sim | Identificador estável do professor na escola |
| `nome_professor` | Sim | Nome do professor |
| `email_professor` | Sim | E-mail usado para login e recuperação de senha |
| `telefone_professor` | Sim | Telefone com DDD, 10 ou 11 dígitos |
| `codigo_aluno` | Sim | Identificador estável do aluno na escola |
| `nome_aluno` | Sim | Nome do aluno |
| `email_aluno` | Sim | E-mail usado para login e recuperação de senha |
| `telefone_aluno` | Sim | Telefone com DDD, 10 ou 11 dígitos |

O `codigo_turma` deve ser diferente para cada combinação de turma e disciplina (por exemplo, `1B-MAT` e `1B-HIST`). Os códigos de escola, turma, professor e aluno devem permanecer estáveis entre importações. O e-mail identifica a conta de login; códigos externos identificam a pessoa dentro da escola.

## Validar e importar

Copie o CSV para um diretório restrito da máquina que acessa o banco de dados da Learn Tech. No backend, compile o projeto e rode primeiro em modo de pré-validação:

```sh
mvn -q -DskipTests package
java -jar target/learn-tech-backend-0.0.1-SNAPSHOT.jar \
  --school.import.file=/caminho-restrito/escola.csv
```

O padrão é `dry-run=true`: valida estrutura, duplicidades internas e incompatibilidades de perfil sem gravar dados. Revise o relatório. Para efetivar, rode o comando novamente com:

```sh
java -jar target/learn-tech-backend-0.0.1-SNAPSHOT.jar \
  --school.import.file=/caminho-restrito/escola.csv \
  --school.import.dry-run=false
```

O importador cria ou atualiza a escola, contas, perfis, turmas, disciplinas e matrículas em uma transação. Reimportar os mesmos códigos atualiza os registros em vez de duplicá-los. Novas contas recebem uma senha aleatória que não é exibida nem incluída no CSV; cada pessoa deve usar **Esqueci minha senha** para definir sua senha. O SMTP precisa estar configurado para recuperação de senha.

Restrinja o acesso ao arquivo e remova-o do diretório de trabalho de acordo com a política de retenção acordada com a escola. O CSV não deve conter senhas.
