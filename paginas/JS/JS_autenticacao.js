/* =========================================================
   LEARN TECH - AUTENTICAÇÃO E REDEFINIÇÃO DE SENHA
   Usado em: autenticacao.html
   ========================================================= */

function setAuthMode(mode) {
  const loginForm = document.getElementById('form-login');
  const cadForm = document.getElementById('form-cadastro');
  const resetForm = document.getElementById('form-reset');
  const title = document.getElementById('auth-main-title');

  if (loginForm) loginForm.classList.add('hidden');
  if (cadForm) cadForm.classList.add('hidden');
  if (resetForm) resetForm.classList.add('hidden');

  if (mode === 'login' && loginForm) {
    loginForm.classList.remove('hidden');
    if (title) title.innerHTML = 'LOGAR <span class="accent-blue">CONTA</span>';
  } else if (mode === 'cadastro' && cadForm) {
    cadForm.classList.remove('hidden');
    if (title) title.innerHTML = 'CRIAR <span class="accent-blue">SUA CONTA</span>';
  } else if (mode === 'reset' && resetForm) {
    resetForm.classList.remove('hidden');
    if (title) title.innerHTML = 'REDEFINIR <span class="accent-blue">SENHA</span>';
  }
}

/* =========================================================
   INTEGRAÇÃO COM O BACK-END (login, cadastro e redefinição)
   ========================================================= */

const TELEFONE_MAXIMO = 2147483647; // a coluna Telefone do banco é INT

function valorDe(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

function entrarNoSistema(usuario) {
  salvarUsuario(usuario);
  window.location.href = 'dashboard.html';
}

async function fazerLogin(evento) {
  evento.preventDefault();

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const email = valorDe('login-email');
    const senhaEl = document.getElementById('login-senha');
    const senha = senhaEl ? senhaEl.value : "";

    const usuario = await entrar(email, senha);
    entrarNoSistema(usuario);
  } catch (erro) {
    alert(erro.message);
    if (botao) botao.disabled = false;
  }
}

async function fazerCadastro(evento) {
  evento.preventDefault();

  const senhaEl = document.getElementById('cad-senha');
  const roleEl = document.getElementById('cad-role');
  const senha = senhaEl ? senhaEl.value : "";
  const role = roleEl ? roleEl.value : "ALUNO";
  const telefone = Number(valorDe('cad-tel').replace(/\D/g, ''));

  if (!telefone || telefone > TELEFONE_MAXIMO) {
    alert('Telefone inválido. Digite apenas números (até 9 dígitos, sem DDD).');
    return;
  }

  if (senha.length < 6) {
    alert('A senha precisa ter pelo menos 6 caracteres.');
    return;
  }

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const usuario = await cadastrar(
      valorDe('cad-nome'),
      valorDe('cad-email'),
      senha,
      role,
      telefone
    );
    entrarNoSistema(usuario);
  } catch (erro) {
    alert(erro.message);
    if (botao) botao.disabled = false;
  }
}

async function redefinirSenha(evento) {
  evento.preventDefault();

  // Mapeamento exato dos IDs do form-reset em autenticacao.html
  const email = valorDe('reset-email');
  const novaSenhaEl = document.getElementById('reset-nova-senha');
  const confSenhaEl = document.getElementById('reset-conf-senha');

  const novaSenha = novaSenhaEl ? novaSenhaEl.value : "";
  const confirmarSenha = confSenhaEl ? confSenhaEl.value : "";

  if (!email || !novaSenha) {
    alert('Preencha o e-mail e a nova senha.');
    return;
  }

  if (novaSenha.length < 6) {
    alert('A nova senha precisa ter pelo menos 6 caracteres.');
    return;
  }

  if (novaSenha !== confirmarSenha) {
    alert('As senhas não coincidem.');
    return;
  }

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    await redefinirSenhaApi(email, novaSenha);
    alert('Senha redefinida com sucesso! Faça login com a sua nova senha.');
    setAuthMode('login');
  } catch (erro) {
    alert(erro.message || 'Erro ao redefinir a senha.');
  } finally {
    if (botao) botao.disabled = false;
  }
}

document.addEventListener('DOMContentLoaded', function () {
  const formLogin = document.getElementById('form-login');
  const formCadastro = document.getElementById('form-cadastro');
  const formReset = document.getElementById('form-reset');

  if (formLogin) formLogin.addEventListener('submit', fazerLogin);
  if (formCadastro) formCadastro.addEventListener('submit', fazerCadastro);
  if (formReset) formReset.addEventListener('submit', redefinirSenha);
});