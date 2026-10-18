/* =========================================================
   LEARN TECH - COMUNICAÇÃO COM O BACK-END
   ========================================================= */

const API = window.location.port === "5500"
  ? `${window.location.protocol}//${window.location.hostname}:8080/api`
  : "/api";

/* ---------- utilitarios ---------- */

function esc(t) {
  return String(t == null ? "" : t).replace(/[&<>"']/g, function (c) {
    return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c];
  });
}

function formatarData(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  return d.toLocaleDateString("pt-BR") + " " + d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

// Rótulos de status atualizados
const ROTULO_STATUS = { 
  DESBLOQUEADO: "DESBLOQUEADO", 
  BLOQUEADO: "BLOQUEADO",
  A_FAZER: "NÃO INICIADO", 
  EM_ANDAMENTO: "EM ANDAMENTO", 
  CONCLUIDA: "CONCLUÍDO" 
};

const ROTULO_PRIORIDADE = { ALTA: "ALTA PRIORIDADE", MEDIA: "MÉDIA PRIORIDADE", BAIXA: "BAIXA PRIORIDADE" };

/* ---------- requisicao base ---------- */

async function request(caminho, opcoes) {
  opcoes = opcoes || {};
  let resposta;
  try {
    resposta = await fetch(API + caminho, {
      headers: { "Content-Type": "application/json" },
      method: opcoes.method || "GET",
      body: opcoes.body ? JSON.stringify(opcoes.body) : undefined,
    });
  } catch (e) {
    throw new Error("Não foi possível conectar ao servidor. Verifique se o back-end está rodando.");
  }
  if (!resposta.ok) {
    let erro = {};
    try { erro = await resposta.json(); } catch (e) {}
    let msg = erro.message || "";
    if (resposta.status >= 500) msg = "Erro no servidor.";
    else if (!msg || msg.indexOf("Validation failed") === 0) msg = "Dados inválidos. Confira os campos.";
    throw new Error(msg);
  }
  const texto = await resposta.text();
  return texto ? JSON.parse(texto) : null;
}

/* ---------- sessao ---------- */

function salvarUsuario(u) { localStorage.setItem("usuario", JSON.stringify(u)); }
function usuarioLogado() {
  try { return JSON.parse(localStorage.getItem("usuario") || "null"); } catch (e) { return null; }
}
function sair() { localStorage.removeItem("usuario"); }
function ehProfessor() { const u = usuarioLogado(); return !!u && u.role === "PROFESSOR"; }

function exigirLogin() { if (!usuarioLogado()) location.href = "autenticacao.html"; }

function aplicarPapel() {
  const u = usuarioLogado();
  const papel = u ? u.role : null;
  document.querySelectorAll("[data-somente]").forEach(function (el) {
    if (el.dataset.somente !== papel) el.style.display = "none";
  });
}
document.addEventListener("DOMContentLoaded", aplicarPapel);

/* ---------- autenticacao e perfil ---------- */

const cadastrar = (nome, email, senha, role, telefone) =>
  request("/auth/cadastro", { 
    method: "POST", 
    body: { 
      nome, 
      email, 
      senha, 
      role, 
      telefone: String(telefone || "").replace(/\D/g, '') 
    } 
  });

const entrar = (email, senha) =>
  request("/auth/login", { method: "POST", body: { email, senha } });

const redefinirSenhaApi = (email, novaSenha) =>
  request("/auth/redefinir-senha", { method: "PUT", body: { email, novaSenha } });

const atualizarPerfil = (id, nome, email) =>
  request("/perfil/" + id, { method: "PUT", body: { nome, email } });

/* ---------- turmas ---------- */

const listarTurmas = () => request("/turmas");
const turmasDoProfessor = (id) => request("/turmas/professor/" + id);
const turmasDoAluno = (id) => request("/turmas/aluno/" + id);
const turmasDisponiveisParaAluno = (alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/turmas/disponiveis?alunoId=" + alunoId);
};
const matricularAlunoNaTurma = (turmaId, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/turmas/" + turmaId + "/matriculas?alunoId=" + alunoId, { method: "POST" });
};

const criarTurma = (quantidade, sala, linkGrupo, professorId) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/turmas", { 
    method: "POST", 
    body: { quantidade, sala, linkGrupo, professorId } 
  });
};

const atualizarTurma = (id, quantidade, sala, linkGrupo) =>
  request("/turmas/" + id, { method: "PUT", body: { quantidade, sala, linkGrupo } });

const excluirTurma = (id) => request("/turmas/" + id, { method: "DELETE" });

/* ---------- disciplinas ---------- */

const disciplinasDaTurma = (turmaId) => request("/disciplinas/turma/" + turmaId);

const criarDisciplina = (professorId, nome, turmaId) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/disciplinas?professorId=" + professorId, { 
    method: "POST", 
    body: { nome, turmaId } 
  });
};

/* ---------- atividades ---------- */

const listarAtividades = () => request("/atividades");
const atividadesDaTurma = (turmaId) => request("/atividades/turma/" + turmaId);

const criarAtividade = (professorId, titulo, descricao, prazo, turmaId, status, dataDesbloqueio) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/atividades?professorId=" + professorId, { 
    method: "POST", 
    body: { titulo, descricao, prazo, turmaId, status, dataDesbloqueio }
  });
};

const atualizarAtividade = (professorId, id, titulo, descricao, prazo, turmaId, status, dataDesbloqueio) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/atividades/" + id + "?professorId=" + professorId, { 
    method: "PUT", 
    body: { titulo, descricao, prazo, turmaId, status, dataDesbloqueio }
  });
};

const excluirAtividade = (professorId, id) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/atividades/" + id + "?professorId=" + professorId, { 
    method: "DELETE" 
  });
};

/* ---------- entregas e notas ---------- */

const enviarEntrega = (atividadeId, textoComentario, arquivoUrl, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "?alunoId=" + alunoId, { 
    method: "POST", 
    body: { textoComentario, arquivoUrl } 
  });
};

const buscarEntregasParaCorrecao = (atividadeId, professorId) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "/detalhes?professorId=" + professorId);
};

const atribuirNotaAluno = (entregaId, nota, professorId) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/entregas/" + entregaId + "/nota?professorId=" + professorId, {
    method: "PUT",
    body: { nota: Number(nota) }
  });
};

const entregasDaAtividade = (atividadeId, professorId) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "?professorId=" + professorId);
};

const minhasNotas = (alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/notas/aluno/" + alunoId);
};

const lancarNota = (professorId, atividadeId, alunoId, nota) => {
  if (!professorId) {
    const u = usuarioLogado();
    professorId = u ? (u.perfilId || u.professorId || u.id) : null;
  }
  return request("/notas?professorId=" + professorId, { 
    method: "POST", 
    body: { atividadeId, alunoId, nota } 
  });
};

/* ---------- post-its ---------- */

const postitsDaAtividade = (atividadeId, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/postits/atividade/" + atividadeId + "?alunoId=" + alunoId);
};

const criarPostIt = (atividadeId, alunoId, titulo, conteudo) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/postits/atividade/" + atividadeId + "?alunoId=" + alunoId, { 
    method: "POST", 
    body: { titulo, conteudo } 
  });
};

const excluirPostIt = (id, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/postits/" + id + "?alunoId=" + alunoId, { 
    method: "DELETE" 
  });
};

/* ---------- notificacoes ---------- */

const notificacoes = (userId) => {
  if (!userId) {
    const u = usuarioLogado();
    userId = u ? (u.usuarioId || u.id) : null;
  }
  return request("/notificacoes/usuario/" + userId);
};

const naoLidas = (userId) => {
  if (!userId) {
    const u = usuarioLogado();
    userId = u ? (u.usuarioId || u.id) : null;
  }
  return request("/notificacoes/usuario/" + userId + "/nao-lidas");
};

const marcarLida = (id, userId) => {
  if (!userId) {
    const u = usuarioLogado();
    userId = u ? (u.usuarioId || u.id) : null;
  }
  return request("/notificacoes/" + id + "/ler?userId=" + userId, { method: "PATCH" });
};

const marcarTodasLidas = (userId) => {
  if (!userId) {
    const u = usuarioLogado();
    userId = u ? (u.usuarioId || u.id) : null;
  }
  return request("/notificacoes/ler-todas?userId=" + userId, { method: "PATCH" });
};

/* ---------- consultas combinadas ---------- */

function estaDesbloqueada(atividade) {
  if (atividade.status === "DESBLOQUEADO") return true;
  if (atividade.status === "BLOQUEADO") {
    if (!atividade.dataDesbloqueio) return false;
    return new Date() >= new Date(atividade.dataDesbloqueio);
  }
  return true;
}

async function minhasTurmas() {
  const u = usuarioLogado();
  if (!u) return [];
  const perfilId = u.perfilId || u.professorId || u.alunoId || u.id;
  return u.role === "PROFESSOR" ? turmasDoProfessor(perfilId) : turmasDoAluno(perfilId);
}

async function minhasAtividades() {
  const u = usuarioLogado();
  if (!u) return [];
  const profId = u.perfilId || u.professorId || u.id;
  const todas = await listarAtividades();
  if (u.role !== "PROFESSOR") return todas;
  const turmas = await turmasDoProfessor(profId);
  const ids = new Set(turmas.map(function (t) { return t.id; }));
  return todas.filter(function (a) { return ids.has(a.turmaId); });
}

async function carregarAgenda() {
  const u = usuarioLogado();
  if (!u) return [];
  const alunoId = u.perfilId || u.alunoId || u.id;
  const atividades = await minhasAtividades();
  const entregas = u.role === "ALUNO" ? await minhasNotas(alunoId) : [];
  const porAtividade = new Map(entregas.map(function (e) { return [e.atividadeId, e]; }));
  
  return atividades.map(function (a) {
    const e = porAtividade.get(a.id);
    const liberada = estaDesbloqueada(a);
    
    let estadoFinal = "NAO_INICIADO";
    if (!liberada) {
      estadoFinal = "BLOQUEADO";
    } else if (e && e.nota !== null) {
      estadoFinal = "CONCLUIDO";
    } else if (e || a.aberta) {
      estadoFinal = "EM_ANDAMENTO";
    }

    return {
      id: a.id,
      nome: a.titulo,
      prazo: a.prazo,
      turmaId: a.turmaId,
      disciplinaNome: a.disciplinaNome,
      entregue: !!e,
      nota: e ? e.nota : null,
      status: estadoFinal,
      prioridade: e ? e.prioridade : "MEDIA",
      desbloqueada: liberada,
      dataDesbloqueio: a.dataDesbloqueio
    };
  }).sort(function (x, y) { return new Date(x.prazo) - new Date(y.prazo); });
}