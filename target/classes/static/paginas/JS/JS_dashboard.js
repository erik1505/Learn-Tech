/* =========================================================
   LEARN TECH - DASHBOARD (dados do back-end)
   Usado em: dashboard.html   Requer: api.js
   ========================================================= */

const COR_PRIORIDADE_DASH = { ALTA: 'red', MEDIA: 'yellow', BAIXA: 'green' };

function cardAgendaDashboard(item) {
  return `
    <div class="timeline-activity-card">
      <div class="activity-name">${esc(item.nome)}</div>
      <div class="activity-due-date">DATA DE ENTREGA: ${esc(formatarData(item.prazo))}</div>
      <div class="activity-status-row">
        <span>STATUS</span>
        <div class="status-select-badge">
          <span>${esc(ROTULO_STATUS[item.status] || item.status)}</span>
          <span>▾</span>
        </div>
      </div>
      <div class="priority-pill ${COR_PRIORIDADE_DASH[item.prioridade] || 'yellow'}">
        <span class="priority-dot"></span>
        <span>${esc(ROTULO_PRIORIDADE[item.prioridade] || item.prioridade)}</span>
        <span class="priority-arrow">▾</span>
      </div>
    </div>`;
}

async function carregarDashboard() {
  const usuario = usuarioLogado();
  const trilha = document.getElementById('dash-timeline');

  try {
    const resultado = await Promise.all([minhasTurmas(), carregarAgenda()]);
    const turmas = resultado[0];
    const agenda = resultado[1];

    document.getElementById('dash-turmas').textContent =
      turmas.length + (turmas.length === 1 ? ' turma' : ' turmas');

    if (usuario.role === 'ALUNO') {
      const pendentes = agenda.filter(function (a) { return !a.entregue; }).length;
      document.getElementById('dash-atividades').textContent =
        pendentes + (pendentes === 1 ? ' atividade pendente' : ' atividades pendentes');

      const notas = agenda.filter(function (a) { return a.entregue && a.nota > 0; }).map(function (a) { return a.nota; });
      document.getElementById('dash-notas').textContent = notas.length
        ? 'Média: ' + (notas.reduce(function (x, y) { return x + y; }, 0) / notas.length).toFixed(1)
        : 'Nenhuma nota ainda';
    } else {
      document.getElementById('dash-atividades').textContent =
        agenda.length + (agenda.length === 1 ? ' atividade cadastrada' : ' atividades cadastradas');
      document.getElementById('dash-notas').textContent = 'Lance as notas em Atividades';
    }

    // Próximas atividades: as que ainda não foram concluídas, por prazo
    const proximas = agenda.filter(function (a) { return a.status !== 'CONCLUIDA'; }).slice(0, 6);

    trilha.innerHTML = proximas.length
      ? proximas.map(cardAgendaDashboard).join('')
      : '<p>Nenhuma atividade pendente.</p>';
  } catch (erro) {
    trilha.innerHTML = '';
    alert(erro.message);
  }
}

document.addEventListener('DOMContentLoaded', carregarDashboard);
