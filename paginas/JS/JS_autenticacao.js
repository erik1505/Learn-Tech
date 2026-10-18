/* =========================================================
   LEARN TECH - AUTENTICAÇÃO E REDEFINIÇÃO DE SENHA
   Usado em: autenticacao.html
   ========================================================= */

function setAuthMode(mode) {
  const loginForm = document.getElementById('form-login');
  const loginCodeForm = document.getElementById('form-login-codigo');
  const cadForm = document.getElementById('form-cadastro');
  const resetForm = document.getElementById('form-reset');
  const title = document.getElementById('auth-main-title');

  if (loginForm) loginForm.classList.add('hidden');
  if (loginCodeForm) loginCodeForm.classList.add('hidden');
  if (cadForm) cadForm.classList.add('hidden');
  if (resetForm) resetForm.classList.add('hidden');

  if (mode !== 'login-codigo') loginChallengeId = null;
  if (mode !== 'login-codigo') {
    const codigoLogin = document.getElementById('login-codigo');
    if (codigoLogin) codigoLogin.value = '';
  }
  if (mode !== 'reset') limparEtapaRedefinicao();

  if (mode === 'login' && loginForm) {
    loginForm.classList.remove('hidden');
    if (title) title.innerHTML = 'LOGAR <span class="accent-blue">CONTA</span>';
  } else if (mode === 'login-codigo' && loginCodeForm) {
    loginCodeForm.classList.remove('hidden');
    if (title) title.innerHTML = 'VERIFIQUE <span class="accent-blue">SEU E-MAIL</span>';
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
let loginChallengeId = null;
let resetChallengeId = null;

function valorDe(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

function entrarNoSistema(usuario) {
  salvarUsuario(usuario);
  window.location.href = 'dashboard.html';
}

function limparEtapaRedefinicao() {
  resetChallengeId = null;
  const emailWrapper = document.getElementById('reset-email-wrapper');
  const codigoWrapper = document.getElementById('reset-codigo-wrapper');
  const senhaWrapper = document.getElementById('reset-nova-senha-wrapper');
  const confirmacaoWrapper = document.getElementById('reset-conf-senha-wrapper');
  const codigo = document.getElementById('reset-codigo');
  const senha = document.getElementById('reset-nova-senha');
  const confirmacao = document.getElementById('reset-conf-senha');
  const botao = document.getElementById('reset-submit');

  if (!emailWrapper) return;
  codigo.value = '';
  senha.value = '';
  confirmacao.value = '';
  emailWrapper.classList.remove('hidden');
  codigoWrapper.classList.add('hidden');
  senhaWrapper.classList.add('hidden');
  confirmacaoWrapper.classList.add('hidden');
  codigo.required = false;
  senha.required = false;
  confirmacao.required = false;
  botao.textContent = 'ENVIAR CÓDIGO';
}

async function fazerLogin(evento) {
  evento.preventDefault();

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const email = valorDe('login-email');
    const senhaEl = document.getElementById('login-senha');
    const senha = senhaEl ? senhaEl.value : "";

    const desafio = await entrar(email, senha);
    if (senhaEl) senhaEl.value = '';
    loginChallengeId = desafio.challengeId;
    document.getElementById('login-codigo-email').textContent = email;
    document.getElementById('login-codigo').value = '';
    setAuthMode('login-codigo');
    if (botao) botao.disabled = false;
  } catch (erro) {
    alert(erro.message);
    if (botao) botao.disabled = false;
  }
}

async function verificarCodigoLogin(evento) {
  evento.preventDefault();
  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const usuario = await verificarCodigoLoginApi(
      loginChallengeId,
      valorDe('login-codigo')
    );
    entrarNoSistema(usuario);
  } catch (erro) {
    alert(erro.message || 'Não foi possível validar o código.');
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
    await cadastrar(
      valorDe('cad-nome'),
      valorDe('cad-email'),
      senha,
      role,
      telefone
    );
    const senhaCadastro = document.getElementById('cad-senha');
    if (senhaCadastro) senhaCadastro.value = '';
    try {
      const desafio = await entrar(valorDe('cad-email'), senha);
      loginChallengeId = desafio.challengeId;
      document.getElementById('login-codigo-email').textContent = valorDe('cad-email');
      document.getElementById('login-codigo').value = '';
      setAuthMode('login-codigo');
    } catch (erro) {
      setAuthMode('login');
      alert(`Conta criada, mas não foi possível enviar o código de acesso. Faça login para tentar novamente. ${erro.message}`);
    }
    if (botao) botao.disabled = false;
  } catch (erro) {
    alert(erro.message);
    if (botao) botao.disabled = false;
  }
}

async function redefinirSenha(evento) {
  evento.preventDefault();

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    if (!resetChallengeId) {
      const desafio = await solicitarCodigoRedefinicaoApi(valorDe('reset-email'));
      resetChallengeId = desafio.challengeId;
      document.getElementById('reset-email-wrapper').classList.add('hidden');
      document.getElementById('reset-codigo-wrapper').classList.remove('hidden');
      document.getElementById('reset-nova-senha-wrapper').classList.remove('hidden');
      document.getElementById('reset-conf-senha-wrapper').classList.remove('hidden');
      document.getElementById('reset-codigo').required = true;
      document.getElementById('reset-nova-senha').required = true;
      document.getElementById('reset-conf-senha').required = true;
      botao.textContent = 'REDEFINIR SENHA';
      alert(desafio.message);
      return;
    }

    const novaSenha = valorDe('reset-nova-senha');
    const confirmarSenha = valorDe('reset-conf-senha');
    if (novaSenha.length < 6) {
      alert('A nova senha precisa ter pelo menos 6 caracteres.');
      return;
    }
    if (novaSenha !== confirmarSenha) {
      alert('As senhas não coincidem.');
      return;
    }

    await redefinirSenhaApi(resetChallengeId, valorDe('reset-codigo'), novaSenha);
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
  const formLoginCodigo = document.getElementById('form-login-codigo');
  const formCadastro = document.getElementById('form-cadastro');
  const formReset = document.getElementById('form-reset');

  if (formLogin) formLogin.addEventListener('submit', fazerLogin);
  if (formLoginCodigo) formLoginCodigo.addEventListener('submit', verificarCodigoLogin);
  if (formCadastro) formCadastro.addEventListener('submit', fazerCadastro);
  if (formReset) formReset.addEventListener('submit', redefinirSenha);
});