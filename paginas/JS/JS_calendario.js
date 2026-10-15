/* =========================================================
   LEARN TECH - CALENDÁRIO / AGENDA (dados do back-end)
   Usado em: calendario.html   Requer: api.js
   ========================================================= */

const CLASSE_PRIORIDADE_CAL = { ALTA: 'alta', MEDIA: 'media', BAIXA: 'baixa' };

function cardAgendaCalendario(item) {
  return `
    <div class="activity-card">
      <div class="card-title">${esc(item.nome)}</div>
      <div class="card-date">DATA DE ENTREGA: ${esc(formatarData(item.prazo))}</div>
      <div class="status-select-container">
        <span class="status-label">STATUS</span>
        <div class="status-dropdown">
          <span>${esc(ROTULO_STATUS[item.status] || item.status)}</span>
          <span>▾</span>
        </div>
      </div>
      <div class="priority-badge ${CLASSE_PRIORIDADE_CAL[item.prioridade] || 'media'}">
        <span class="priority-dot"></span>
        <span>${esc(ROTULO_PRIORIDADE[item.prioridade] || item.prioridade)}</span>
        <span class="priority-arrow">▾</span>
      </div>
    </div>`;
}

async function carregarCalendario() {
  const linha = document.getElementById('agenda-cards');

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

document.addEventListener('DOMContentLoaded', carregarCalendario);
