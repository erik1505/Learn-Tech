/* =========================================================
   LEARN TECH - CALENDÁRIO / AGENDA (dados do back-end)
   Usado em: calendario.html   Requer: api.js
   ========================================================= */

const CLASSE_PRIORIDADE_CAL = { ALTA: 'alta', MEDIA: 'media', BAIXA: 'baixa' };

function cardAgendaCalendario(item) {
  const usuario = usuarioLogado();
  const prioridade = ['ALTA', 'MEDIA', 'BAIXA'].includes(item.prioridade)
    ? item.prioridade
    : 'MEDIA';
  const controlePrioridade = usuario && usuario.role === 'ALUNO'
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
    <div class="activity-card">
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
    const agenda = await carregarAgenda();

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
  carregarCalendario();
});
