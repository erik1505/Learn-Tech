/* =========================================================
   LEARN TECH - NOTAS (dados do back-end)
   Usado em: notas.html   Requer: api.js

   No banco a nota fica na entrega (Entrega_atividade.Nota).
   Nota 0 significa "entregue, aguardando correção".
   ========================================================= */

function pontuacaoDaAtividade(atividade, entrega) {
  const nota = entrega && entrega.nota != null
    ? entrega.nota
    : atividade.nota != null ? atividade.nota : atividade.pontuacao;

  if (nota == null || nota === '') return null;

  const valor = Number(nota);
  return Number.isFinite(valor) && valor > 0 ? valor : null;
}

function formatarPontuacao(nota) {
  return new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 }).format(nota);
}

function textoDaNota(atividade, entrega, pontuacao) {
  if (pontuacao != null) return formatarPontuacao(pontuacao) + ' pts';

  const notaPendente = (entrega && entrega.nota != null && Number(entrega.nota) === 0)
    || (atividade.nota != null && Number(atividade.nota) === 0);
  if (notaPendente || (entrega && entrega.statusKanban === 'CONCLUIDA')) {
    return 'Aguardando correção';
  }

  return 'Não entregue';
}

function montarCardMateria(nomeMateria, atividades, entregas) {
  const linhas = atividades.map(function (atividade) {
    const entrega = entregas.get(atividade.id) || entregas.get(String(atividade.id));
    const pontuacao = pontuacaoDaAtividade(atividade, entrega);
    const textoPontuacao = textoDaNota(atividade, entrega, pontuacao);

    return {
      atividade: atividade,
      pontuacao: pontuacao,
      textoPontuacao: textoPontuacao
    };
  });

  const pontuacoesLancadas = linhas.filter(function (linha) {
    return linha.pontuacao != null;
  });
  const total = pontuacoesLancadas.reduce(function (soma, linha) {
    return soma + linha.pontuacao;
  }, 0);
  const totalTexto = formatarPontuacao(total);
  const resumoNota = pontuacoesLancadas.length ? totalTexto : '—';
  const resumoLegenda = pontuacoesLancadas.length ? 'PONTOS' : 'SEM NOTAS';
  const resumoQuantidade = pontuacoesLancadas.length + ' de ' + linhas.length + ' atividades com nota';

  const listaAtividades = linhas.map(function (linha) {
    const notaPendente = linha.pontuacao == null;
    return `
      <div class="grade-row">
        <span class="grade-activity-name">${esc(linha.atividade.titulo || linha.atividade.nome || 'Atividade')}</span>
        <span class="grade-score${notaPendente ? ' grade-score-pending' : ''}">${esc(linha.textoPontuacao)}</span>
      </div>
    `;
  }).join('');

  return `
    <details class="subject-card">
      <summary class="subject-summary">
        <span class="subject-summary-copy">
          <span class="subject-eyebrow">NOTA GERAL</span>
          <strong class="subject-name">${esc(nomeMateria)}</strong>
          <span class="subject-graded-count">${esc(resumoQuantidade)}</span>
        </span>
        <span class="subject-summary-score">
          <strong>${esc(resumoNota)}</strong>
          <span>${esc(resumoLegenda)}</span>
        </span>
        <span class="subject-chevron" aria-hidden="true"></span>
      </summary>
      <div class="subject-details">
        <div class="grade-list">${listaAtividades}</div>
        <div class="subject-total">
          <span>Total das notas lançadas</span>
          <strong>${esc(totalTexto)} pts</strong>
        </div>
      </div>
    </details>
  `;
}

function renderizarNotas(grade, atividades, entregas) {
  const porDisciplina = new Map();

  atividades.forEach(function (atividade) {
    const nomeMateria = atividade.disciplinaNome || 'Outras atividades';
    const lista = porDisciplina.get(nomeMateria) || [];
    lista.push(atividade);
    porDisciplina.set(nomeMateria, lista);
  });

  if (!porDisciplina.size) {
    grade.innerHTML = '<p>Nenhuma atividade cadastrada ainda.</p>';
    return;
  }

  grade.innerHTML = Array.from(porDisciplina.entries()).map(function (par) {
    return montarCardMateria(par[0], par[1], entregas);
  }).join('');
}

async function carregarNotas() {
  const usuario = usuarioLogado();
  const grade = document.getElementById('notas-grid');

  if (usuario && usuario.modoDemonstracao) {
    try {
      const atividades = JSON.parse(localStorage.getItem('learntech_atividades_db') || '[]')
        .filter(function (atividade) { return Number(atividade.turmaId) === 900001; });
      renderizarNotas(grade, atividades, new Map());
    } catch (erro) {
      grade.innerHTML = '<p>Não foi possível carregar as notas de demonstração.</p>';
    }
    return;
  }

  if (!usuario) {
    grade.innerHTML = '<p>Entre na sua conta para consultar as notas.</p>';
    return;
  }

  if (usuario.role !== 'ALUNO') {
    grade.innerHTML = '<p>As notas são exibidas para alunos. Para lançar notas, abra uma atividade.</p>';
    return;
  }

  try {
    const resultado = await Promise.all([minhasNotas(usuario.id), listarAtividades()]);
    const entregas = new Map(resultado[0].map(function (e) { return [e.atividadeId, e]; }));
    const atividades = resultado[1];
    renderizarNotas(grade, atividades, entregas);
  } catch (erro) {
    grade.innerHTML = '';
    alert(erro.message);
  }
}

document.addEventListener('DOMContentLoaded', carregarNotas);
