/* =========================================================
   LEARN TECH - CONTROLE DA PÁGINA DE TURMAS
   ========================================================= */

let turmasCache = [];
let turmaEditandoId = null;

document.addEventListener("DOMContentLoaded", function () {
  carregarMinhasTurmas();

  // Fecha menus suspenso contextuais ao clicar fora deles
  document.addEventListener("click", function () {
    document.querySelectorAll(".context-menu-dropdown").forEach(el => el.classList.remove("active"));
  });
});

function showScreen(screen) {
  if (screen === "turmas") {
    document.getElementById("screen-turmas").classList.remove("hidden");
    document.getElementById("screen-edicao").classList.add("hidden");
  } else if (screen === "edicao") {
    document.getElementById("screen-turmas").classList.add("hidden");
    document.getElementById("screen-edicao").classList.remove("hidden");
  }
}

async function carregarMinhasTurmas() {
  const container = document.getElementById("turmas-lista");
  const disponiveisContainer = document.getElementById("turmas-disponiveis");
  const tituloDisponiveis = document.getElementById("titulo-turmas-disponiveis");
  const usuario = usuarioLogado();
  try {
    if (usuario && usuario.role === "ALUNO") {
      const resultado = await Promise.all([minhasTurmas(), turmasDisponiveisParaAluno()]);
      turmasCache = resultado[0];
      disponiveisContainer.innerHTML = resultado[1].length
        ? resultado[1].map(t => cardTurmaHTML(t, true)).join("")
        : "<p>Nenhuma turma disponível para entrar.</p>";
      tituloDisponiveis.classList.remove("hidden");
    } else {
      turmasCache = await minhasTurmas();
      disponiveisContainer.innerHTML = "";
      tituloDisponiveis.classList.add("hidden");
    }
  } catch (err) {
    console.error("Não foi possível carregar as turmas do servidor.", err);
    turmasCache = [];
    disponiveisContainer.innerHTML = "";
    container.innerHTML = `<p>Erro ao carregar as turmas: ${esc(err.message || "falha de conexão")}</p>`;
    return;
  }

  if (!turmasCache || turmasCache.length === 0) {
    container.innerHTML = "<p>Nenhuma turma encontrada.</p>";
    return;
  }

  container.innerHTML = turmasCache.map(t => cardTurmaHTML(t)).join("");
}

/* RENDERIZAÇÃO DOS CARDS COM CLASSES E ESTRUTURA CSS */
function cardTurmaHTML(t, disponivel = false) {
  const professor = typeof ehProfessor === 'function' ? ehProfessor() : true;
  const safeSala = typeof esc === 'function' ? esc(t.sala) : t.sala;
  const safeProf = typeof esc === 'function' ? esc(t.professorNome || "DOCENTE") : (t.professorNome || "DOCENTE");
  const safeLink = typeof esc === 'function' ? esc(t.linkGrupo || "#") : (t.linkGrupo || "#");

  return `
    <div class="turma-card">
      <div class="turma-card-header">
        <div class="turma-card-left">
          <div class="turma-avatar"></div>
          <div class="turma-info">
            <h4>${safeSala}</h4>
            <p>${t.quantidade || 0} ALUNOS · PROF. ${safeProf}</p>
          </div>
        </div>
        <div class="options-container">
          <button type="button" class="btn-options" onclick="toggleMenuTurma(event, ${t.id})">⋮</button>
          <div id="menu-turma-${t.id}" class="context-menu-dropdown">
            ${professor ? `<button type="button" class="context-item" onclick="editarTurmaTela(${t.id})">EDITAR</button>` : ""}
            ${professor ? `<button type="button" class="context-item" onclick="excluirTurmaTela(${t.id})">EXCLUIR</button>` : ""}
            <button type="button" class="context-item" onclick="window.open('${safeLink}', '_blank')">ENTRAR NO GRUPO</button>
          </div>
        </div>
      </div>
      ${disponivel
        ? `<button type="button" class="btn-ver-atividade" onclick="matricularNaTurma(${t.id}, this)">ENTRAR NA TURMA</button>`
        : `<a href="atividades.html?turma=${t.id}" class="btn-ver-atividade">Ver atividade</a>`}
    </div>
  `;
}

async function matricularNaTurma(turmaId, botao) {
  botao.disabled = true;
  try {
    await matricularAlunoNaTurma(turmaId);
    alert("Matrícula realizada! A turma e suas atividades já estão disponíveis.");
    await carregarMinhasTurmas();
  } catch (err) {
    alert("Não foi possível entrar na turma: " + err.message);
    botao.disabled = false;
  }
}

function toggleMenuTurma(event, id) {
  event.stopPropagation();
  const menu = document.getElementById(`menu-turma-${id}`);
  const estaAtivo = menu.classList.contains("active");

  document.querySelectorAll(".context-menu-dropdown").forEach(m => m.classList.remove("active"));

  if (!estaAtivo) {
    menu.classList.add("active");
  }
}

function novaTurma() {
  turmaEditandoId = null;
  document.getElementById("nome-turma").value = "";
  document.getElementById("qtd-turma").value = "";
  document.getElementById("link-grupo").value = "";
  showScreen("edicao");
}

function editarTurmaTela(id) {
  const turma = turmasCache.find(t => t.id === id);
  if (!turma) return;

  turmaEditandoId = id;
  document.getElementById("nome-turma").value = turma.sala;
  document.getElementById("qtd-turma").value = turma.quantidade;
  document.getElementById("link-grupo").value = turma.linkGrupo;
  showScreen("edicao");
}

async function salvarTurma() {
  const sala = document.getElementById("nome-turma").value.trim();
  const quantidade = parseInt(document.getElementById("qtd-turma").value, 10);
  const linkGrupo = document.getElementById("link-grupo").value.trim();

  if (!sala || isNaN(quantidade) || !linkGrupo) {
    alert("Preencha todos os campos do formulário.");
    return;
  }

  try {
    if (turmaEditandoId) {
      if (!modoDemonstracaoAtivo() && typeof atualizarTurma === 'function') {
        await atualizarTurma(turmaEditandoId, quantidade, sala, linkGrupo);
      }
      const idx = turmasCache.findIndex(t => t.id === turmaEditandoId);
      if (idx !== -1) turmasCache[idx] = { ...turmasCache[idx], sala, quantidade, linkGrupo };
      alert("Turma atualizada com sucesso!");
    } else {
      const nova = { id: Date.now(), sala, quantidade, professorNome: "DOCENTE", linkGrupo };
      if (!modoDemonstracaoAtivo() && typeof criarTurma === 'function') {
        await criarTurma(quantidade, sala, linkGrupo);
      }
      turmasCache.push(nova);
      alert("Turma criada com sucesso!");
    }
    if (modoDemonstracaoAtivo()) {
      localStorage.setItem("learntech_turmas_demo", JSON.stringify(turmasCache));
    }
    carregarMinhasTurmas();
    showScreen("turmas");
  } catch (err) {
    alert("Erro ao salvar turma: " + err.message);
  }
}

async function excluirTurmaTela(id) {
  if (!confirm("Tem certeza que deseja excluir esta turma?")) return;

  try {
    if (!modoDemonstracaoAtivo() && typeof excluirTurma === 'function') await excluirTurma(id);
    turmasCache = turmasCache.filter(t => t.id !== id);
    if (modoDemonstracaoAtivo()) {
      localStorage.setItem("learntech_turmas_demo", JSON.stringify(turmasCache));
    }
    alert("Turma excluída com sucesso!");
    carregarMinhasTurmas();
  } catch (err) {
    alert("Não foi possível excluir a turma: " + err.message);
  }
}