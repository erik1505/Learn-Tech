/* =========================================================
   LEARN TECH - CONTROLE DA PÁGINA DE TURMAS
   ========================================================= */

let turmasCache = [];
let turmaEditandoId = null;

document.addEventListener("DOMContentLoaded", function () {
  const convitePendente = new URLSearchParams(window.location.search).get("convite");
  if (convitePendente && !usuarioLogado()) {
    sessionStorage.setItem("learnTech.convite", convitePendente);
    window.location.href = "autenticacao.html";
    return;
  }
  if (convitePendente) {
    entrarPorConvite(convitePendente);
  } else {
    carregarMinhasTurmas();
  }

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
  try {
    turmasCache = await minhasTurmas();
  } catch (err) {
    console.error("Não foi possível carregar as turmas do servidor.", err);
    turmasCache = [];
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
function cardTurmaHTML(t) {
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
            ${professor ? `<button type="button" class="context-item" onclick="copiarLinkConvite('${esc(t.codigoConvite || "")}')">COPIAR CONVITE</button>` : ""}
            ${professor ? `<button type="button" class="context-item" onclick="verIntegrantesTurma(${t.id})">VER INTEGRANTES</button>` : ""}
            <button type="button" class="context-item" onclick="window.open('${safeLink}', '_blank')">ENTRAR NO GRUPO</button>
          </div>
        </div>
      </div>
      <a href="atividades.html?turma=${t.id}" class="btn-ver-atividade">Ver atividade</a>
    </div>
  `;
}

async function verIntegrantesTurma(turmaId) {
  const modal = document.getElementById("integrantes-modal");
  const conteudo = document.getElementById("integrantes-conteudo");
  const turma = turmasCache.find(item => item.id === turmaId);
  document.getElementById("integrantes-titulo").textContent =
    "Integrantes — " + (turma ? turma.sala : "Turma");
  conteudo.innerHTML = "<p>Carregando integrantes...</p>";
  modal.classList.remove("hidden");
  modal.setAttribute("aria-hidden", "false");

  try {
    const integrantes = await integrantesDaTurma(turmaId);
    if (!integrantes.length) {
      conteudo.innerHTML = "<p>Nenhum aluno matriculado nesta turma.</p>";
      return;
    }

    conteudo.innerHTML = `
      <div class="integrantes-tabela-wrapper">
        <table class="integrantes-tabela">
          <thead><tr><th>Nome</th><th>RA</th><th>E-mail</th><th>Média das notas</th></tr></thead>
          <tbody>${integrantes.map(aluno => `
            <tr>
              <td>${esc(aluno.nome)}</td>
              <td>${esc(aluno.ra)}</td>
              <td>${esc(aluno.email)}</td>
              <td>${aluno.mediaNota == null ? "—" : esc(new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 2 }).format(aluno.mediaNota))}</td>
            </tr>`).join("")}
          </tbody>
        </table>
      </div>`;
  } catch (err) {
    conteudo.innerHTML = `<p>Erro ao carregar integrantes: ${esc(err.message || "falha de conexão")}</p>`;
  }
}

function fecharIntegrantesTurma() {
  const modal = document.getElementById("integrantes-modal");
  modal.classList.add("hidden");
  modal.setAttribute("aria-hidden", "true");
}

async function entrarPorConvite(codigo) {
  try {
    await entrarEmTurmaPorConvite(codigo);
    sessionStorage.removeItem("learnTech.convite");
    history.replaceState(null, "", window.location.pathname);
    alert("Você entrou na turma com sucesso.");
    await carregarMinhasTurmas();
  } catch (erro) {
    alert("Não foi possível entrar na turma: " + erro.message);
  }
}

async function copiarLinkConvite(codigo) {
  if (!codigo) return;
  const link = new URL("turmas.html?convite=" + encodeURIComponent(codigo), window.location.href).href;
  try {
    await navigator.clipboard.writeText(link);
    alert("Link de convite copiado.");
  } catch {
    window.prompt("Copie o link de convite:", link);
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
      if (typeof atualizarTurma === 'function') {
        await atualizarTurma(turmaEditandoId, quantidade, sala, linkGrupo);
      }
      const idx = turmasCache.findIndex(t => t.id === turmaEditandoId);
      if (idx !== -1) turmasCache[idx] = { ...turmasCache[idx], sala, quantidade, linkGrupo };
      alert("Turma atualizada com sucesso!");
    } else {
      let nova;
      if (typeof criarTurma === 'function') {
        nova = await criarTurma(quantidade, sala, linkGrupo);
      }
      if (nova) turmasCache.push(nova);
      alert("Turma criada com sucesso!");
    }
    await carregarMinhasTurmas();
    showScreen("turmas");
  } catch (err) {
    alert("Erro ao salvar turma: " + err.message);
  }
}

async function excluirTurmaTela(id) {
  if (!confirm("Tem certeza que deseja excluir esta turma?")) return;

  try {
    if (typeof excluirTurma === 'function') await excluirTurma(id);
    turmasCache = turmasCache.filter(t => t.id !== id);
    alert("Turma excluída com sucesso!");
    await carregarMinhasTurmas();
  } catch (err) {
    alert("Não foi possível excluir a turma: " + err.message);
  }
}