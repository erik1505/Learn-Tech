/* =========================================================
   LEARN TECH - NAVEGAÇÃO, ATIVIDADES, CORREÇÃO E MODO FOCO
   ========================================================= */

let atividadesTurma = [];
let entregasAluno = new Map();
let disciplinasTurma = [];
let atividadeAtualId = null;
let atividadeEmEdicaoId = null;
let ficheiroReferenciaTemp = null;
let postItAtualId = null;

let entregasAtividadeAtual = [];
let entregaSelecionadaId = null;
let arquivoAlunoBase64 = null;

let listaPostIts = [];

const TAMANHO_MAXIMO_BYTES = 25 * 1024 * 1024;
const STORAGE_KEY_ATIVIDADES = 'learntech_atividades_db';
const STORAGE_KEY_LEARNBOOK = 'learntech_learnbook_notes';
const STORAGE_KEY_POSTITS = 'learntech_postits_db';

const DEMO_ATIVIDADES = [
  { 
    id: 1, 
    titulo: "ATIVIDADE DEMO 1", 
    status: "A_FAZER", 
    prazo: "2027-06-15T23:59", 
    descricao: "Esta é uma atividade de demonstração.",
    arquivoNome: null,
    arquivoUrl: null
  },
  { 
    id: 2, 
    titulo: "ATIVIDADE DEMO 2", 
    status: "A_FAZER", 
    prazo: "2027-06-15T23:59", 
    descricao: "Descrição da atividade 2.",
    arquivoNome: null,
    arquivoUrl: null
  }
];

const DEMO_POSTITS = [
  { id: 1, titulo: "AULA 1", texto: "ANOTAÇÕES DA AULA 1..." },
  { id: 2, titulo: "AULA 2", texto: "ANOTAÇÕES DA AULA 2..." }
];

function porId(id) {
  return document.getElementById(id);
}

/* CARREGAR E SALVAR DADOS NO LOCALSTORAGE */
function carregarAtividadesSalvas() {
  const dados = localStorage.getItem(STORAGE_KEY_ATIVIDADES);
  if (dados) {
    try {
      atividadesTurma = JSON.parse(dados);
    } catch (e) {
      atividadesTurma = [...DEMO_ATIVIDADES];
    }
  } else {
    atividadesTurma = [...DEMO_ATIVIDADES];
    salvarNoLocalStorage();
  }

  const notasSalvas = localStorage.getItem(STORAGE_KEY_LEARNBOOK);
  if (notasSalvas) {
    const textarea = porId('learn-book-notes');
    if (textarea) textarea.value = notasSalvas;
  }

  const dadosPostIts = localStorage.getItem(STORAGE_KEY_POSTITS);
  if (dadosPostIts) {
    try {
      listaPostIts = JSON.parse(dadosPostIts);
    } catch (e) {
      listaPostIts = [...DEMO_POSTITS];
    }
  } else {
    listaPostIts = [...DEMO_POSTITS];
    localStorage.setItem(STORAGE_KEY_POSTITS, JSON.stringify(listaPostIts));
  }

  renderizarPostIts();
}

function salvarNoLocalStorage() {
  localStorage.setItem(STORAGE_KEY_ATIVIDADES, JSON.stringify(atividadesTurma));
}

function salvarNotasLearnBook(texto) {
  localStorage.setItem(STORAGE_KEY_LEARNBOOK, texto);
}

function salvarPostItsNoStorage() {
  localStorage.setItem(STORAGE_KEY_POSTITS, JSON.stringify(listaPostIts));
}

/* NAVEGAÇÃO SPA DIRETA */
function navigateTo(screenTarget, pushHistory = true) {
  const telaTrilha = porId('screen-trilha-turma');
  const telaDetalhe = porId('screen-detalhe-atividade');
  const telaEdicao = porId('screen-edicao-atividade');

  if (telaTrilha) telaTrilha.classList.add('hidden');
  if (telaDetalhe) telaDetalhe.classList.add('hidden');
  if (telaEdicao) telaEdicao.classList.add('hidden');

  const btnModoFoco = porId('btn-modo-foco');

  if (screenTarget === 'trilha-turma') {
    desativarModoFocoGeral();
    if (btnModoFoco) btnModoFoco.style.display = 'none';
    if (telaTrilha) telaTrilha.classList.remove('hidden');
  } else if (screenTarget === 'detalhe-atividade') {
    if (btnModoFoco) btnModoFoco.style.display = 'block';
    if (telaDetalhe) telaDetalhe.classList.remove('hidden');
  } else if (screenTarget === 'edicao-atividade') {
    desativarModoFocoGeral();
    if (btnModoFoco) btnModoFoco.style.display = 'none';
    if (telaEdicao) telaEdicao.classList.remove('hidden');
  }

  if (pushHistory) {
    history.pushState({ screen: screenTarget, atividadeId: atividadeAtualId }, '', `#${screenTarget}`);
  }
}

window.addEventListener('popstate', function (event) {
  if (event.state && event.state.screen) {
    if (event.state.atividadeId && event.state.screen === 'detalhe-atividade') {
      verAtividade(event.state.atividadeId, false);
    } else {
      navigateTo(event.state.screen, false);
    }
  } else {
    navigateTo('trilha-turma', false);
  }
});

/* CRIAR NOVA ATIVIDADE */
function novaAtividade() {
  atividadeEmEdicaoId = null;

  const titForm = porId('titulo-form-edicao');
  if (titForm) titForm.textContent = 'NOVA';

  const inputNome = porId('edit-nome');
  const inputDesc = porId('edit-desc');
  const inputPrazo = porId('edit-prazo');
  const inputArq = porId('edit-arquivo-input');
  const displayArq = porId('edit-nome-arquivo-selecionado');

  if (inputNome) inputNome.value = '';
  if (inputDesc) inputDesc.value = '';
  if (inputPrazo) inputPrazo.value = '';
  if (inputArq) inputArq.value = '';
  if (displayArq) displayArq.textContent = 'Nenhum arquivo selecionado';

  ficheiroReferenciaTemp = null;

  const selectDisc = porId('edit-disciplina');
  if (selectDisc) {
    const lista = (disciplinasTurma && disciplinasTurma.length) 
      ? disciplinasTurma 
      : [{ id: 1, nome: "Geral" }];
    selectDisc.innerHTML = lista.map(d => `<option value="${d.id}">${d.nome}</option>`).join('');
  }

  navigateTo('edicao-atividade');
}

/* EDITAR ATIVIDADE */
function editarAtividade(id) {
  const atividade = atividadesTurma.find(a => a.id === id);
  if (!atividade) return;

  atividadeEmEdicaoId = id;

  const titForm = porId('titulo-form-edicao');
  if (titForm) titForm.textContent = 'EDIÇÃO DA';

  porId('edit-nome').value = atividade.titulo || '';
  porId('edit-desc').value = atividade.descricao || '';
  porId('edit-prazo').value = atividade.prazo ? atividade.prazo.substring(0, 16) : '';
  porId('edit-status').value = atividade.status === 'BLOQUEADO' ? 'BLOQUEADO' : 'A_FAZER';

  if (atividade.arquivoNome) {
    porId('edit-nome-arquivo-selecionado').textContent = atividade.arquivoNome;
    ficheiroReferenciaTemp = {
      nome: atividade.arquivoNome,
      base64: atividade.arquivoUrl
    };
  } else {
    porId('edit-nome-arquivo-selecionado').textContent = 'Nenhum arquivo selecionado';
    ficheiroReferenciaTemp = null;
  }

  const selectDisc = porId('edit-disciplina');
  if (selectDisc) {
    const lista = disciplinasTurma.length ? disciplinasTurma : [{ id: 1, nome: "Geral" }];
    selectDisc.innerHTML = lista.map(d => `<option value="${d.id}">${d.nome}</option>`).join('');
  }

  navigateTo('edicao-atividade');
}

/* DELETAR ATIVIDADE LOCAL */
function deletarAtividadeLocal(id) {
  if (confirm("Tem certeza que deseja excluir esta atividade?")) {
    atividadesTurma = atividadesTurma.filter(a => a.id !== id);
    salvarNoLocalStorage();
    renderizarTrilha();
  }
}

async function salvarAtividade() {
  const nome = porId('edit-nome').value.trim();
  const descricao = porId('edit-desc').value.trim();
  const prazo = porId('edit-prazo').value;
  const statusSelect = porId('edit-status').value;

  if (!nome || !descricao || !prazo) {
    alert('Preencha todos os campos obrigatórios do formulário.');
    return;
  }

  let arquivoNome = null;
  let arquivoUrl = null;

  if (ficheiroReferenciaTemp) {
    arquivoNome = ficheiroReferenciaTemp.nome;
    arquivoUrl = ficheiroReferenciaTemp.base64;
  }

  if (atividadeEmEdicaoId) {
    const atividade = atividadesTurma.find(a => a.id === atividadeEmEdicaoId);
    if (atividade) {
      atividade.titulo = nome;
      atividade.descricao = descricao;
      atividade.prazo = prazo;
      atividade.status = statusSelect;
      if (arquivoNome) {
        atividade.arquivoNome = arquivoNome;
        atividade.arquivoUrl = arquivoUrl;
      }
    }
  } else {
    const nova = {
      id: Date.now(),
      titulo: nome,
      descricao: descricao,
      prazo: prazo,
      status: statusSelect || 'A_FAZER',
      arquivoNome: arquivoNome,
      arquivoUrl: arquivoUrl
    };
    atividadesTurma.push(nova);
  }

  salvarNoLocalStorage();

  alert('Atividade salva com sucesso!');
  renderizarTrilha();
  navigateTo('trilha-turma');
}

/* CONTROLES DO MODO FOCO E DA INTERFACE */
function togglePainelEnvio() {
  const painel = porId('painel-envio');
  if (painel) painel.classList.toggle('hidden');
}

function toggleModoFoco() {
  const estaAtivo = document.body.classList.toggle('modo-foco-ativo');
  const btnFoco = porId('btn-modo-foco');
  if (btnFoco) btnFoco.classList.toggle('active', estaAtivo);

  const painelLearnBook = porId('painel-learn-book');
  const painelEnvio = porId('painel-envio');

  if (estaAtivo) {
    if (painelEnvio) painelEnvio.classList.add('hidden');
    if (painelLearnBook) painelLearnBook.classList.remove('hidden');
  } else {
    if (painelLearnBook) painelLearnBook.classList.add('hidden');
    fecharPostItExpandido();
  }
}

function desativarModoFocoGeral() {
  document.body.classList.remove('modo-foco-ativo');
  const btnFoco = porId('btn-modo-foco');
  if (btnFoco) btnFoco.classList.remove('active');

  const painelLearnBook = porId('painel-learn-book');
  if (painelLearnBook) painelLearnBook.classList.add('hidden');
  fecharPostItExpandido();
}

function renderizarPostIts() {
  const container = porId('postits-container');
  if (!container) return;

  container.innerHTML = listaPostIts.map(p => `
    <div class="postit-item postit-thumb" onclick="selecionarPostIt(${p.id})">
      <div class="postit-pin postit-striped-tape"></div>
      <span>${p.titulo}</span>
    </div>
  `).join('');
}

function selecionarPostIt(id) {
  const postit = listaPostIts.find(p => p.id === id);
  if (!postit) return;

  postItAtualId = id;

  const padrao = porId('conteudo-atividade-padrao');
  const expandido = porId('conteudo-postit-expandido');
  const textareaExpandido = porId('postit-expanded-textarea');

  if (padrao && expandido && textareaExpandido) {
    textareaExpandido.value = postit.texto || '';
    padrao.classList.add('hidden');
    expandido.classList.remove('hidden');
  }
}

function atualizarTextoPostIt(novoTexto) {
  if (!postItAtualId) return;

  const postit = listaPostIts.find(p => p.id === postItAtualId);
  if (postit) {
    postit.texto = novoTexto;
    salvarPostItsNoStorage();
  }
}

function fecharPostItExpandido() {
  postItAtualId = null;
  const padrao = porId('conteudo-atividade-padrao');
  const expandido = porId('conteudo-postit-expandido');

  if (padrao && expandido) {
    expandido.classList.add('hidden');
    padrao.classList.remove('hidden');
  }
}

function criarNovoPostIt() {
  const num = listaPostIts.length + 1;
  const novo = {
    id: Date.now(),
    titulo: `AULA ${num}`,
    texto: `ANOTAÇÕES DA AULA ${num}...`
  };

  listaPostIts.push(novo);
  salvarPostItsNoStorage();
  renderizarPostIts();
}

function atualizarNomeArquivo(input) {
  const display = porId('nome-arquivo-selecionado');
  if (input.files && input.files[0]) {
    const file = input.files[0];
    display.textContent = file.name;

    const reader = new FileReader();
    reader.onload = function (e) {
      arquivoAlunoBase64 = e.target.result;
    };
    reader.readAsDataURL(file);
  } else {
    display.textContent = 'Nenhum arquivo selecionado';
    arquivoAlunoBase64 = null;
  }
}

function validarEAtualizarArquivoReferencia(input) {
  const display = porId('edit-nome-arquivo-selecionado');

  if (input.files && input.files[0]) {
    const file = input.files[0];

    if (file.size > TAMANHO_MAXIMO_BYTES) {
      alert("O arquivo selecionado excede o tamanho máximo permitido de 25 MB.");
      input.value = "";
      ficheiroReferenciaTemp = null;
      display.textContent = "Nenhum arquivo selecionado";
      return;
    }

    const reader = new FileReader();
    reader.onload = function (e) {
      ficheiroReferenciaTemp = {
        nome: file.name,
        base64: e.target.result
      };
      display.textContent = `${file.name} (${(file.size / (1024 * 1024)).toFixed(2)} MB)`;
    };
    reader.readAsDataURL(file);

  } else {
    ficheiroReferenciaTemp = null;
    display.textContent = "Nenhum arquivo selecionado";
  }
}

function atualizarBarraProgresso() {
  const total = atividadesTurma.length;

  if (total === 0) {
    porId('progresso-fill').style.width = '0%';
    porId('progresso-valor').textContent = '0% COMPLETO (0 DE 0)';
    return;
  }

  const concluidas = atividadesTurma.filter(a => a.status === 'CONCLUIDA' || entregasAluno.has(a.id)).length;
  const porcentagem = Math.round((concluidas / total) * 100);

  const fillBar = porId('progresso-fill');
  const valorTxt = porId('progresso-valor');

  if (fillBar) fillBar.style.width = porcentagem + '%';
  if (valorTxt) valorTxt.textContent = `${porcentagem}% COMPLETO (${concluidas} DE ${total})`;
}

function toggleMenuOpcoes(event, id) {
  event.stopPropagation();

  document.querySelectorAll('.dropdown-menu-card').forEach(menu => {
    if (menu.id !== `dropdown-menu-${id}`) {
      menu.classList.add('hidden');
    }
  });

  const menuTarget = porId(`dropdown-menu-${id}`);
  if (menuTarget) {
    menuTarget.classList.toggle('hidden');
  }
}

document.addEventListener('click', function () {
  document.querySelectorAll('.dropdown-menu-card').forEach(menu => {
    menu.classList.add('hidden');
  });
});

function cardAtividadeHTML(a) {
  let classeStatus = 'card-nao-iniciado';
  let textoStatus = 'Não iniciado';

  if (a.status === 'CONCLUIDA' || entregasAluno.has(a.id)) {
    classeStatus = 'card-concluido';
    textoStatus = 'Concluido';
  } else if (a.status === 'EM_ANDAMENTO') {
    classeStatus = 'card-andamento';
    textoStatus = 'Em andamento';
  } else if (a.status === 'BLOQUEADO') {
    classeStatus = 'card-bloqueada';
    textoStatus = 'Bloqueada';
  }

  const safeTitulo = typeof esc === 'function' ? esc(a.titulo) : a.titulo;

  return `
    <div class="activity-card ${classeStatus}">
      <div class="card-top">
        <div class="card-meta-left">
          <div class="classroom-avatar"></div>
          <div class="card-texts">
            <h4>${safeTitulo}</h4>
            <p>${textoStatus}</p>
          </div>
        </div>
        <div class="card-dots-wrapper">
          <button class="btn-card-dots" onclick="toggleMenuOpcoes(event, ${a.id})">⋮</button>
          <div id="dropdown-menu-${a.id}" class="dropdown-menu-card hidden">
            <button type="button" onclick="editarAtividade(${a.id})">EDITAR</button>
            <button type="button" class="btn-delete-option" onclick="deletarAtividadeLocal(${a.id})">EXCLUIR</button>
          </div>
        </div>
      </div>
      <button class="btn-ver-atividade" onclick="verAtividade(${a.id})">VER ATIVIDADE</button>
    </div>
  `;
}

function renderizarTrilha() {
  const grade = porId('atividades-grid');
  if (!grade) return;

  grade.innerHTML = atividadesTurma.map(cardAtividadeHTML).join('');
  atualizarBarraProgresso();
}

async function carregarTrilha() {
  carregarAtividadesSalvas();

  try {
    if (typeof minhasTurmas === 'function') {
      const turmas = await minhasTurmas();
      if (turmas && turmas.length) {
        const turmaPedida = Number(new URLSearchParams(location.search).get('turma'));
        const turmaAtual = turmas.find(t => t.id === turmaPedida) || turmas[0];
        porId('turma-titulo').textContent = turmaAtual.sala;

        try {
          disciplinasTurma = await disciplinasDaTurma(turmaAtual.id);
        } catch (e) {
          disciplinasTurma = [{ id: 1, nome: "Geral" }];
        }
      }
    }
  } catch (err) {
    console.warn("Utilizando armazenamento local.");
  }
  renderizarTrilha();
}

function verAtividade(id, pushHistory = true) {
  const atividade = atividadesTurma.find(a => a.id === id);
  if (!atividade) return;

  atividadeAtualId = id;

  if (atividade.status === 'A_FAZER') {
    atividade.status = 'EM_ANDAMENTO';
    salvarNoLocalStorage();
    renderizarTrilha();
  }

  porId('atividade-titulo').textContent = atividade.titulo;
  porId('envio-atividade-titulo').textContent = atividade.titulo;
  porId('atividade-descricao').textContent = atividade.descricao || "Sem descrição disponível.";

  const nomeArquivoEl = porId('card-ref-nome-arquivo');
  const btnBaixarRef = porId('btn-baixar-referencia');

  if (atividade.arquivoNome && atividade.arquivoUrl) {
    nomeArquivoEl.textContent = atividade.arquivoNome;
    btnBaixarRef.href = atividade.arquivoUrl;
    btnBaixarRef.download = atividade.arquivoNome;
    btnBaixarRef.classList.remove('hidden');
  } else {
    nomeArquivoEl.textContent = "ARQUIVO ENVIADO PARA REFERÊNCIA";
    btnBaixarRef.classList.add('hidden');
  }

  const u = usuarioLogado();
  const btnEnvio = porId('btn-abrir-envio');
  if (btnEnvio) {
    btnEnvio.style.display = (u && u.role === 'ALUNO') ? 'block' : 'none';
  }

  const estaEntregue = atividade.status === 'CONCLUIDA' || entregasAluno.has(id);
  const statusTag = porId('atividade-status-tag');
  if (statusTag) {
    statusTag.textContent = estaEntregue ? 'CONCLUÍDO' : 'PENDENTE';
    statusTag.className = estaEntregue ? 'status-badge-concluido' : 'status-badge-pendente';
  }

  carregarCorrecoesProfessor(id);
  fecharPostItExpandido();
  navigateTo('detalhe-atividade', pushHistory);
}

async function enviarTarefa() {
  const texto = porId('texto-usuario-envio').value.trim();

  if (!texto && !arquivoAlunoBase64) {
    alert('Por favor, digite um texto ou anexe um arquivo para enviar.');
    return;
  }

  try {
    await enviarEntrega(atividadeAtualId, texto, arquivoAlunoBase64);
    alert('Tarefa enviada com sucesso!');

    const ativ = atividadesTurma.find(a => a.id === atividadeAtualId);
    if (ativ) {
      ativ.status = 'CONCLUIDA';
      salvarNoLocalStorage();
    }
    entregasAluno.set(atividadeAtualId, { data: new Date() });

    porId('painel-envio').classList.add('hidden');
    renderizarTrilha();
    verAtividade(atividadeAtualId, false);
  } catch (e) {
    alert(e.message || 'Erro ao enviar tarefa.');
  }
}

/* LÓGICA DE CORREÇÃO DO PROFESSOR */
async function carregarCorrecoesProfessor(atividadeId) {
  const u = usuarioLogado();
  const painelCorrecao = porId('painel-correcao-professor');

  if (!u || u.role !== 'PROFESSOR') {
    if (painelCorrecao) painelCorrecao.classList.add('hidden');
    return;
  }

  if (painelCorrecao) painelCorrecao.classList.remove('hidden');

  try {
    entregasAtividadeAtual = await buscarEntregasParaCorrecao(atividadeId);
    renderizarListaAlunosCorrecao();
  } catch (e) {
    entregasAtividadeAtual = [];
    renderizarListaAlunosCorrecao();
  }
}

function renderizarListaAlunosCorrecao() {
  const lista = porId('lista-alunos-entregas');
  if (!lista) return;

  if (!entregasAtividadeAtual || entregasAtividadeAtual.length === 0) {
    lista.innerHTML = `<p style="color: #94a3b8; font-size: 13px;">Nenhuma entrega realizada até o momento.</p>`;
    porId('visualizador-envio-aluno').classList.add('hidden');
    return;
  }

  lista.innerHTML = entregasAtividadeAtual.map(e => `
    <div onclick="selecionarEntregaAluno(${e.entregaId})" class="aluno-item-btn" style="background: ${entregaSelecionadaId === e.entregaId ? '#2563eb' : '#0f121d'};">
      <span><strong>${esc(e.alunoNome)}</strong></span>
      <span style="font-size: 11px; font-weight: bold; background: rgba(255,255,255,0.2); padding: 3px 8px; border-radius: 12px;">
        ${e.nota !== null && e.nota !== undefined ? 'Nota: ' + e.nota : 'Pendente'}
      </span>
    </div>
  `).join('');
}

function selecionarEntregaAluno(entregaId) {
  const entrega = entregasAtividadeAtual.find(e => e.entregaId === entregaId);
  if (!entrega) return;

  entregaSelecionadaId = entregaId;
  renderizarListaAlunosCorrecao();

  const visualizador = porId('visualizador-envio-aluno');
  visualizador.classList.remove('hidden');

  porId('nome-aluno-selecionado').textContent = entrega.alunoNome;
  porId('email-aluno-selecionado').textContent = entrega.alunoEmail;
  porId('texto-aluno-selecionado').textContent = entrega.textoComentario || "Nenhum texto informado pelo aluno.";

  const btnBaixar = porId('btn-baixar-arquivo-aluno');
  const nomeArq = porId('nome-arquivo-aluno');

  if (entrega.arquivoUrl) {
    nomeArq.textContent = "Documento em anexo disponível";
    btnBaixar.href = entrega.arquivoUrl;
    btnBaixar.classList.remove('hidden');
  } else {
    nomeArq.textContent = "Nenhum arquivo em anexo.";
    btnBaixar.classList.add('hidden');
  }

  porId('input-nota-aluno').value = entrega.nota !== null && entrega.nota !== undefined ? entrega.nota : '';
}

async function salvarNotaAluno() {
  const notaVal = porId('input-nota-aluno').value;

  if (notaVal === '' || isNaN(notaVal) || notaVal < 0 || notaVal > 10) {
    alert('Informe uma nota válida entre 0 e 10.');
    return;
  }

  try {
    await atribuirNotaAluno(entregaSelecionadaId, notaVal);
    alert('Nota atribuída com sucesso!');

    const entrega = entregasAtividadeAtual.find(e => e.entregaId === entregaSelecionadaId);
    if (entrega) entrega.nota = Number(notaVal);

    renderizarListaAlunosCorrecao();
  } catch (e) {
    alert(e.message || 'Erro ao atribuir nota.');
  }
}

document.addEventListener('DOMContentLoaded', function () {
  carregarTrilha();

  const btnNova = porId('btn-nova-atividade');
  if (btnNova) {
    btnNova.addEventListener('click', novaAtividade);
  }
});