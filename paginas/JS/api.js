/* =========================================================
   LEARN TECH - COMUNICAÇÃO COM O BACK-END
   ========================================================= */

const codespaceHost = window.location.hostname.match(/^(.*)-\d+\.app\.github\.dev$/);
const API = window.location.port === "5500"
  ? `${window.location.protocol}//${window.location.hostname}:8080/api`
  : codespaceHost
    ? `${window.location.protocol}//${codespaceHost[1]}-8080.app.github.dev/api`
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
    if (resposta.status >= 500 && !msg) msg = "Erro no servidor.";
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
function modoDemonstracaoAtivo() {
  const usuario = usuarioLogado();
  return !!(usuario && usuario.modoDemonstracao);
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
function aplicarDestinoLogos() {
  if (!usuarioLogado()) return;

  const destinoDashboard = window.location.pathname.includes("/paginas/")
    ? "dashboard.html"
    : "./paginas/dashboard.html";

  document.querySelectorAll("a").forEach(function (link) {
    if (
      link.matches(".brand, .brand-logo, .footer-logo") ||
      link.querySelector(".sidebar-logo")
    ) {
      link.href = destinoDashboard;
    }
  });
}

document.addEventListener("DOMContentLoaded", function () {
  aplicarPapel();
  aplicarDestinoLogos();
});

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

const enviarCodigoLoginApi = (challengeId, canal) =>
  request("/auth/login/enviar-codigo", { method: "POST", body: { challengeId, canal } });

const verificarCodigoLoginApi = (challengeId, codigo) =>
  request("/auth/login/verificar-codigo", { method: "POST", body: { challengeId, codigo } });

const solicitarCodigoRedefinicaoApi = (email) =>
  request("/auth/redefinir-senha/codigo", { method: "POST", body: { email } });

const redefinirSenhaApi = (challengeId, codigo, novaSenha) =>
  request("/auth/redefinir-senha", { method: "PUT", body: { challengeId, codigo, novaSenha } });

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

const iniciarAtividadeAluno = (atividadeId, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "/iniciar?alunoId=" + alunoId, {
    method: "POST"
  });
};

const sairAtividadeAluno = (atividadeId, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "/sair?alunoId=" + alunoId, {
    method: "POST"
  });
};

const atualizarPrioridadeAtividade = (atividadeId, prioridade, alunoId) => {
  if (!alunoId) {
    const u = usuarioLogado();
    alunoId = u ? (u.perfilId || u.alunoId || u.id) : null;
  }
  return request("/entregas/atividade/" + atividadeId + "/prioridade?alunoId=" + alunoId, {
    method: "PUT",
    body: { prioridade }
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
  if (u.modoDemonstracao) {
    try {
      return JSON.parse(localStorage.getItem("learntech_turmas_demo") || "[]");
    } catch (e) {
      return [];
    }
  }
  const perfilId = u.perfilId || u.professorId || u.alunoId || u.id;
  return u.role === "PROFESSOR" ? turmasDoProfessor(perfilId) : turmasDoAluno(perfilId);
}

function entrarModoDemonstracao() {
  const turmaId = 900001;
  const chaveTurmas = "learntech_turmas_demo";
  const chaveAtividades = "learntech_atividades_db";
  let turmas = [];
  let atividades = [];

  try { turmas = JSON.parse(localStorage.getItem(chaveTurmas) || "[]"); } catch (e) {}
  try { atividades = JSON.parse(localStorage.getItem(chaveAtividades) || "[]"); } catch (e) {}

  if (!turmas.some(turma => turma.id === turmaId)) {
    turmas.push({
      id: turmaId,
      sala: "Turma de demonstração",
      quantidade: 24,
      professorNome: "Professor de teste",
      linkGrupo: "#"
    });
  }

  if (!atividades.some(atividade => atividade.id === turmaId)) {
    atividades.push({
      id: turmaId,
      turmaId,
      titulo: "Atividade de demonstração",
      descricao: "Atividade padrão para testar a navegação, edição e recursos da turma.",
      prazo: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      status: "A_FAZER",
      disciplinaNome: "Matemática",
      nota: 8.5,
      persistida: false
    });
  } else {
    const atividadeDemo = atividades.find(atividade => atividade.id === turmaId);
    atividadeDemo.disciplinaNome = atividadeDemo.disciplinaNome || "Matemática";
    if (atividadeDemo.nota == null) atividadeDemo.nota = 8.5;
  }

  localStorage.setItem(chaveTurmas, JSON.stringify(turmas));
  localStorage.setItem(chaveAtividades, JSON.stringify(atividades));
  salvarUsuario({
    id: turmaId,
    perfilId: turmaId,
    nome: "Professor de demonstração",
    email: "professor.demo@learntech.local",
    role: "PROFESSOR",
    modoDemonstracao: true
  });
  window.location.href = "turmas.html";
}

async function minhasAtividades() {
  const u = usuarioLogado();
  if (!u) return [];
  const profId = u.perfilId || u.professorId || u.id;
  const todas = await listarAtividades();
  const turmas = u.role === "PROFESSOR"
    ? await turmasDoProfessor(profId)
    : await turmasDoAluno(profId);
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
    
    let estadoFinal = a.status || "A_FAZER";
    if (u.role === "ALUNO" && !liberada) {
      estadoFinal = "BLOQUEADO";
    } else if (u.role === "ALUNO" && e && (e.statusKanban === "CONCLUIDA" || e.nota !== null)) {
      estadoFinal = "CONCLUIDA";
    } else if (u.role === "ALUNO" && e && e.statusKanban === "EM_ANDAMENTO") {
      estadoFinal = "EM_ANDAMENTO";
    } else if (u.role === "ALUNO" && e && e.statusKanban === "A_FAZER") {
      estadoFinal = "A_FAZER";
    } else if (u.role === "ALUNO" && !e) {
      estadoFinal = "A_FAZER";
    }

    return {
      id: a.id,
      nome: a.titulo,
      prazo: a.prazo,
      turmaId: a.turmaId,
      disciplinaNome: a.disciplinaNome,
      entregue: !!e && (e.statusKanban === "CONCLUIDA" || e.nota !== null),
      nota: e ? e.nota : null,
      status: estadoFinal,
      prioridade: e && e.prioridade ? e.prioridade : "MEDIA",
      desbloqueada: liberada,
      dataDesbloqueio: a.dataDesbloqueio
    };
  }).sort(function (x, y) { return new Date(x.prazo) - new Date(y.prazo); });
}