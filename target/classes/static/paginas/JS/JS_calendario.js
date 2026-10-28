/* =========================================================
   LEARN TECH - CALENDÁRIO / AGENDA (dados do back-end)
   Usado em: calendario.html   Requer: api.js
   ========================================================= */

const CLASSE_PRIORIDADE_CAL = { ALTA: 'alta', MEDIA: 'media', BAIXA: 'baixa' };
const AGENDA_DEMO = new URLSearchParams(window.location.search).get('demo') === '1'
  || modoDemonstracaoAtivo();
const CHAVE_PRIORIDADES_DEMO = 'learntech_agenda_demo_prioridades';

function atividadesAgendaDemo() {
  const agora = new Date();
  const atividades = [
    { id: 'demo-1', nome: 'Revisão de funções', prazoDias: 1, prazoHoras: 18, disciplinaNome: 'Matemática', status: 'EM_ANDAMENTO', prioridade: 'ALTA' },
    { id: 'demo-2', nome: 'Resumo: Revolução Industrial', prazoDias: 2, prazoHoras: 12, disciplinaNome: 'História', status: 'A_FAZER', prioridade: 'MEDIA' },
    { id: 'demo-3', nome: 'Exercícios de cinemática', prazoDias: 4, prazoHoras: 9, disciplinaNome: 'Física', status: 'A_FAZER', prioridade: 'BAIXA' },
    { id: 'demo-4', nome: 'Redação: tecnologia e sociedade', prazoDias: 6, prazoHoras: 20, disciplinaNome: 'Língua Portuguesa', status: 'CONCLUIDA', prioridade: 'MEDIA' },
    { id: 'demo-5', nome: 'Pesquisa sobre ecossistemas', prazoDias: 9, prazoHoras: 16, disciplinaNome: 'Biologia', status: 'A_FAZER', prioridade: 'ALTA' },
    { id: 'demo-teste', nome: 'Atividade teste do calendário', prazoDias: 3, prazoHoras: 15, disciplinaNome: 'Demonstração', status: 'A_FAZER', prioridade: 'MEDIA', testeDemo: true }
  ];

  let prioridades = {};
  try { prioridades = JSON.parse(localStorage.getItem(CHAVE_PRIORIDADES_DEMO) || '{}'); } catch (erro) {}

  return atividades.map(function (atividade) {
    const prazo = new Date(agora);
    prazo.setDate(prazo.getDate() + atividade.prazoDias);
    prazo.setHours(atividade.prazoHoras, 0, 0, 0);
    return Object.assign({}, atividade, {
      prazo: prazo.toISOString(),
      prioridade: prioridades[atividade.id] || atividade.prioridade
    });
  });
}

function cardAgendaCalendario(item) {
  const usuario = usuarioLogado();
  const prioridade = ['ALTA', 'MEDIA', 'BAIXA'].includes(item.prioridade)
    ? item.prioridade
    : 'MEDIA';
  const controlePrioridade = (AGENDA_DEMO || usuario && usuario.role === 'ALUNO')
    ? `<label class="priority-control">
        <span>MINHA PRIORIDADE</span>
        <select class="priority-badge ${CLASSE_PRIORIDADE_CAL[prioridade]}" data-atividade-id="${esc(item.id)}" aria-label="Prioridade para ${esc(item.nome)}">
          <option value="BAIXA" ${prioridade === 'BAIXA' ? 'selected' : ''}>BAIXA</option>
          <option value="MEDIA" ${prioridade === 'MEDIA' ? 'selected' : ''}>MÉDIA</option>
          <option value="ALTA" ${prioridade === 'ALTA' ? 'selected' : ''}>ALTA</option>
        </select>
      </label>`
    : `<div class="priority-badge ${CLASSE_PRIORIDADE_CAL[prioridade] || 'media'}">
        <span class="priority-dot"></span>
        <span>${esc(ROTULO_PRIORIDADE[prioridade] || prioridade)}</span>
      </div>`;

  return `
    <div class="activity-card${AGENDA_DEMO && item.testeDemo ? ' demo-test-card' : ''}">
      ${AGENDA_DEMO && item.testeDemo ? '<span class="demo-test-badge">ATIVIDADE DE TESTE</span>' : ''}
      <div class="card-title">${esc(item.nome)}</div>
      <div class="card-subject">${esc(item.disciplinaNome || 'Atividade')}</div>
      <div class="card-date">DATA DE ENTREGA: ${esc(formatarData(item.prazo))}</div>
      <div class="status-select-container">
        <span class="status-label">STATUS</span>
        <span class="status-dropdown">${esc(ROTULO_STATUS[item.status] || item.status || 'NÃO INICIADO')}</span>
      </div>
      ${controlePrioridade}
    </div>`;
}

async function alterarPrioridadeCalendario(evento) {
  const seletor = evento.target.closest('select[data-atividade-id]');
  if (!seletor) return;

  if (AGENDA_DEMO) {
    let prioridades = {};
    try { prioridades = JSON.parse(localStorage.getItem(CHAVE_PRIORIDADES_DEMO) || '{}'); } catch (erro) {}
    prioridades[seletor.dataset.atividadeId] = seletor.value;
    localStorage.setItem(CHAVE_PRIORIDADES_DEMO, JSON.stringify(prioridades));
    seletor.className = `priority-badge ${CLASSE_PRIORIDADE_CAL[seletor.value]}`;
    return;
  }

  seletor.disabled = true;

  try {
    await atualizarPrioridadeAtividade(seletor.dataset.atividadeId, seletor.value);
    seletor.className = `priority-badge ${CLASSE_PRIORIDADE_CAL[seletor.value]}`;
  } catch (erro) {
    alert(erro.message || 'Não foi possível atualizar a prioridade.');
    await carregarCalendario();
  } finally {
    seletor.disabled = false;
  }
}

async function carregarCalendario() {
  const linha = document.getElementById('agenda-cards');
  linha.innerHTML = '<p>Carregando atividades...</p>';

  try {
    const agenda = AGENDA_DEMO ? atividadesAgendaDemo() : await carregarAgenda();

    linha.innerHTML = agenda.length
      ? agenda.map(cardAgendaCalendario).join('')
      : '<p>Nenhuma atividade na agenda.</p>';
  } catch (erro) {
    linha.innerHTML = '';
    alert(erro.message);
  }
}

document.addEventListener('DOMContentLoaded', function () {
  const linha = document.getElementById('agenda-cards');
  if (linha) linha.addEventListener('change', alterarPrioridadeCalendario);
  if (AGENDA_DEMO) document.getElementById('agenda-demo-notice').hidden = false;
  carregarCalendario();
});
