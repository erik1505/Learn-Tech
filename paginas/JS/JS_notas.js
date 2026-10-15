/* =========================================================
   LEARN TECH - NOTAS (dados do back-end)
   Usado em: notas.html   Requer: api.js

   No banco a nota fica na entrega (Entrega_atividade.Nota).
   Nota 0 significa "entregue, aguardando correção".
   ========================================================= */

function textoDaNota(atividade, entrega) {
  if (!entrega) {
    return 'Não entregue';
  }

  return entrega.nota > 0 ? String(entrega.nota) : 'Aguardando correção';
}

async function carregarNotas() {
  const usuario = usuarioLogado();
  const grade = document.getElementById('notas-grid');

  if (usuario.role !== 'ALUNO') {
    grade.innerHTML = '<p>As notas são exibidas para alunos. Para lançar notas, abra uma atividade.</p>';
    return;
  }

  try {
    const resultado = await Promise.all([minhasNotas(usuario.id), listarAtividades()]);
    const entregas = new Map(resultado[0].map(function (e) { return [e.atividadeId, e]; }));
    const atividades = resultado[1];

    // Agrupa as atividades por disciplina
    const porDisciplina = new Map();

    atividades.forEach(function (atividade) {
      const lista = porDisciplina.get(atividade.disciplinaNome) || [];
      lista.push(atividade);
      porDisciplina.set(atividade.disciplinaNome, lista);
    });

    if (!porDisciplina.size) {
      grade.innerHTML = '<p>Nenhuma atividade cadastrada ainda.</p>';
      return;
    }

    grade.innerHTML = Array.from(porDisciplina.entries()).map(function (par) {
      const linhas = par[1].map(function (a) {
        return '<div class="grade-row">' + esc(a.titulo) + ': ' + esc(textoDaNota(a, entregas.get(a.id))) + '</div>';
      }).join('');

      return '<article class="subject-card"><h2>' + esc(par[0]) + '</h2>' + linhas + '</article>';
    }).join('');
  } catch (erro) {
    grade.innerHTML = '';
    alert(erro.message);
  }
}

document.addEventListener('DOMContentLoaded', carregarNotas);
