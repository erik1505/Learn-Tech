/* =========================================================
   LEARN TECH - AUTENTICAÇÃO E REDEFINIÇÃO DE SENHA
   Usado em: autenticacao.html
   ========================================================= */

function setAuthMode(mode) {
  const loginForm = document.getElementById('form-login');
  const loginChannelForm = document.getElementById('form-login-canal');
  const loginCodeForm = document.getElementById('form-login-codigo');
  const cadForm = document.getElementById('form-cadastro');
  const resetForm = document.getElementById('form-reset');
  const title = document.getElementById('auth-main-title');

  if (loginForm) loginForm.classList.add('hidden');
  if (loginChannelForm) loginChannelForm.classList.add('hidden');
  if (loginCodeForm) loginCodeForm.classList.add('hidden');
  if (cadForm) cadForm.classList.add('hidden');
  if (resetForm) resetForm.classList.add('hidden');

  if (mode !== 'login-canal' && mode !== 'login-codigo') loginChallengeId = null;
  if (mode !== 'login-codigo') {
    sessionStorage.removeItem('learnTech.loginCodeChallenge');
  }
  if (mode !== 'login-canal') {
    sessionStorage.removeItem('learnTech.loginChannelChallenge');
  }
  if (mode !== 'reset') {
    sessionStorage.removeItem('learnTech.passwordResetChallenge');
  }
  if (mode !== 'login-codigo') {
    const codigoLogin = document.getElementById('login-codigo');
    if (codigoLogin) codigoLogin.value = '';
  }
  if (mode !== 'reset') limparEtapaRedefinicao();

  if (mode === 'login' && loginForm) {
    loginForm.classList.remove('hidden');
    if (title) title.innerHTML = 'LOGAR <span class="accent-blue">CONTA</span>';
  } else if (mode === 'login-canal' && loginChannelForm) {
    loginChannelForm.classList.remove('hidden');
    if (title) title.innerHTML = 'CONFIRME <span class="accent-blue">SUA IDENTIDADE</span>';
  } else if (mode === 'login-codigo' && loginCodeForm) {
    loginCodeForm.classList.remove('hidden');
    if (title) title.innerHTML = 'VERIFIQUE <span class="accent-blue">SUA CONTA</span>';
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

let loginChallengeId = null;
let resetChallengeId = null;
let resetStep = 'email';

function valorDe(id) {
  const el = document.getElementById(id);
  return el ? el.value.trim() : "";
}

function entrarNoSistema(usuario) {
  salvarUsuario(usuario);
  window.location.href = 'dashboard.html';
}

function mostrarCarregamentoAuth(mensagem) {
  const carregamento = document.getElementById('auth-loading');
  document.getElementById('auth-loading-message').textContent = mensagem;
  carregamento.classList.remove('hidden');
  carregamento.setAttribute('aria-hidden', 'false');
}

function esconderCarregamentoAuth() {
  const carregamento = document.getElementById('auth-loading');
  carregamento.classList.add('hidden');
  carregamento.setAttribute('aria-hidden', 'true');
}

function limparEtapaRedefinicao() {
  resetChallengeId = null;
  sessionStorage.removeItem('learnTech.passwordResetChallenge');
  const emailWrapper = document.getElementById('reset-email-wrapper');
  const email = document.getElementById('reset-email');
  const channelWrapper = document.getElementById('reset-channel-wrapper');
  const codigoWrapper = document.getElementById('reset-codigo-wrapper');
  const senhaWrapper = document.getElementById('reset-nova-senha-wrapper');
  const confirmacaoWrapper = document.getElementById('reset-conf-senha-wrapper');
  const codigo = document.getElementById('reset-codigo');
  const senha = document.getElementById('reset-nova-senha');
  const confirmacao = document.getElementById('reset-conf-senha');
  const botao = document.getElementById('reset-submit');
  const status = document.getElementById('reset-status');
  const backEmail = document.getElementById('reset-back-email');

  if (!emailWrapper) return;
  resetStep = 'email';
  codigo.value = '';
  senha.value = '';
  confirmacao.value = '';
  emailWrapper.classList.remove('hidden');
  email.required = true;
  channelWrapper.classList.add('hidden');
  codigoWrapper.classList.add('hidden');
  senhaWrapper.classList.add('hidden');
  confirmacaoWrapper.classList.add('hidden');
  codigo.required = false;
  senha.required = false;
  confirmacao.required = false;
  document.querySelector('input[name="reset-canal"][value="EMAIL"]').checked = true;
  botao.textContent = 'CONTINUAR';
  backEmail.classList.add('hidden');
  status.textContent = '';
  status.classList.add('hidden');
}

function voltarEmailRedefinicao() {
  if (resetStep !== 'channel') return;

  resetStep = 'email';
  document.getElementById('reset-channel-wrapper').classList.add('hidden');
  document.getElementById('reset-email-wrapper').classList.remove('hidden');
  document.getElementById('reset-email').required = true;
  document.getElementById('reset-back-email').classList.add('hidden');
  document.getElementById('reset-submit').textContent = 'CONTINUAR';
}

function mostrarOpcoesCanalLogin(desafio) {
  loginChallengeId = desafio.challengeId;
  document.getElementById('login-email-mascarado').textContent = desafio.emailMascarado;
  document.getElementById('login-telefone-mascarado').textContent = desafio.telefoneMascarado || '';
  document.getElementById('login-sms-option').hidden = !desafio.smsDisponivel;
  document.querySelector('input[name="login-canal"][value="EMAIL"]').checked = true;
  sessionStorage.setItem('learnTech.loginChannelChallenge', JSON.stringify({
    challengeId: desafio.challengeId,
    emailMascarado: desafio.emailMascarado,
    telefoneMascarado: desafio.telefoneMascarado || '',
    smsDisponivel: desafio.smsDisponivel,
    expiraEm: Date.now() + 5 * 60 * 1000
  }));
  setAuthMode('login-canal');
}

function restaurarEtapaLogin() {
  const dadosSalvos = sessionStorage.getItem('learnTech.loginCodeChallenge');
  if (dadosSalvos) {
    let desafio;
    try {
      desafio = JSON.parse(dadosSalvos);
    } catch {
      sessionStorage.removeItem('learnTech.loginCodeChallenge');
    }

    if (desafio
        && typeof desafio.challengeId === 'string'
        && typeof desafio.destino === 'string'
        && Number.isFinite(desafio.expiraEm)
        && desafio.expiraEm > Date.now()) {
      loginChallengeId = desafio.challengeId;
      document.getElementById('login-codigo-destino').textContent = desafio.destino;
      document.getElementById('login-codigo-status').textContent = desafio.mensagem || '';
      setAuthMode('login-codigo');
      return;
    }
    sessionStorage.removeItem('learnTech.loginCodeChallenge');
  }

  const dadosCanal = sessionStorage.getItem('learnTech.loginChannelChallenge');
  if (!dadosCanal) return;

  let desafioCanal;
  try {
    desafioCanal = JSON.parse(dadosCanal);
  } catch {
    sessionStorage.removeItem('learnTech.loginChannelChallenge');
    return;
  }

  if (!desafioCanal
      || typeof desafioCanal.challengeId !== 'string'
      || typeof desafioCanal.emailMascarado !== 'string'
      || typeof desafioCanal.telefoneMascarado !== 'string'
      || typeof desafioCanal.smsDisponivel !== 'boolean'
      || !Number.isFinite(desafioCanal.expiraEm)
      || desafioCanal.expiraEm <= Date.now()) {
    sessionStorage.removeItem('learnTech.loginChannelChallenge');
    return;
  }

  loginChallengeId = desafioCanal.challengeId;
  document.getElementById('login-email-mascarado').textContent = desafioCanal.emailMascarado;
  document.getElementById('login-telefone-mascarado').textContent = desafioCanal.telefoneMascarado;
  document.getElementById('login-sms-option').hidden = !desafioCanal.smsDisponivel;
  document.querySelector('input[name="login-canal"][value="EMAIL"]').checked = true;
  setAuthMode('login-canal');
}

async function fazerLogin(evento) {
  evento.preventDefault();

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const email = valorDe('login-identificador');
    const senhaEl = document.getElementById('login-senha');
    const senha = senhaEl ? senhaEl.value : "";

    const desafio = await entrar(email, senha);
    if (senhaEl) senhaEl.value = '';
    mostrarOpcoesCanalLogin(desafio);
    if (botao) botao.disabled = false;
  } catch (erro) {
    alert(erro.message);
    if (botao) botao.disabled = false;
  }
}

async function enviarCodigoLogin(evento) {
  evento.preventDefault();
  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const canal = document.querySelector('input[name="login-canal"]:checked').value;
    mostrarCarregamentoAuth(canal === 'EMAIL' ? 'Enviando o código por e-mail...' : 'Enviando o código por SMS...');
    const resposta = await enviarCodigoLoginApi(loginChallengeId, canal);
    if (!resposta || !resposta.challengeId) {
      throw new Error('Não foi possível confirmar o envio do código. Tente novamente.');
    }
    loginChallengeId = resposta.challengeId;
    const destino = canal === 'SMS'
      ? document.getElementById('login-telefone-mascarado').textContent
      : document.getElementById('login-email-mascarado').textContent;
    sessionStorage.setItem('learnTech.loginCodeChallenge', JSON.stringify({
      challengeId: resposta.challengeId,
      destino,
      mensagem: resposta.message,
      expiraEm: Date.now() + 10 * 60 * 1000
    }));
    document.getElementById('login-codigo-destino').textContent = destino;
    document.getElementById('login-codigo-status').textContent = resposta.message;
    document.getElementById('login-codigo').value = '';
    setAuthMode('login-codigo');
    document.getElementById('login-codigo').focus();
  } catch (erro) {
    alert(erro.message || 'Não foi possível enviar o código.');
  } finally {
    esconderCarregamentoAuth();
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
    sessionStorage.removeItem('learnTech.loginCodeChallenge');
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
  const telefone = valorDe('cad-tel').replace(/\D/g, '');

  if (!/^[1-9][0-9]{9,10}$/.test(telefone)) {
    alert('Telefone inválido. Informe o DDD e um número com 10 ou 11 dígitos.');
    return;
  }

  if (senha.length < 6) {
    alert('A senha precisa ter pelo menos 6 caracteres.');
    return;
  }

  const botao = evento.target.querySelector('button[type="submit"]');
  if (botao) botao.disabled = true;

  try {
    const email = valorDe('cad-email');
    await cadastrar(
      valorDe('cad-nome'),
      email,
      senha,
      role,
      telefone
    );
    const senhaCadastro = document.getElementById('cad-senha');
    if (senhaCadastro) senhaCadastro.value = '';
    const emailLogin = document.getElementById('login-identificador');
    const senhaLogin = document.getElementById('login-senha');
    if (emailLogin) emailLogin.value = email;
    if (senhaLogin) senhaLogin.value = '';
    setAuthMode('login');
    alert('Conta criada com sucesso! Faça login para continuar.');
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
    if (resetStep === 'email') {
      resetStep = 'channel';
      document.getElementById('reset-email-wrapper').classList.add('hidden');
      document.getElementById('reset-email').required = false;
      document.getElementById('reset-channel-wrapper').classList.remove('hidden');
      document.getElementById('reset-back-email').classList.remove('hidden');
      botao.textContent = 'ENVIAR CÓDIGO';
      return;
    }

    if (!resetChallengeId) {
      const canal = document.querySelector('input[name="reset-canal"]:checked').value;
      mostrarCarregamentoAuth(canal === 'EMAIL'
        ? 'Enviando o código de redefinição por e-mail...'
        : 'Enviando o código de redefinição por SMS...');
      const desafio = await solicitarCodigoRedefinicaoApi(valorDe('reset-email'), canal);
      if (!desafio || !desafio.challengeId) {
        throw new Error('Não foi possível confirmar o envio do código. Tente novamente.');
      }
      resetChallengeId = desafio.challengeId;
      sessionStorage.setItem('learnTech.passwordResetChallenge', JSON.stringify({
        challengeId: desafio.challengeId,
        mensagem: desafio.message,
        expiraEm: Date.now() + 10 * 60 * 1000
      }));
      resetStep = 'code';
      document.getElementById('reset-channel-wrapper').classList.add('hidden');
      document.getElementById('reset-codigo-wrapper').classList.remove('hidden');
      document.getElementById('reset-nova-senha-wrapper').classList.remove('hidden');
      document.getElementById('reset-conf-senha-wrapper').classList.remove('hidden');
      document.getElementById('reset-codigo').required = true;
      document.getElementById('reset-nova-senha').required = true;
      document.getElementById('reset-conf-senha').required = true;
      botao.textContent = 'REDEFINIR SENHA';
      const status = document.getElementById('reset-status');
      status.textContent = desafio.message;
      status.classList.remove('hidden');
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
    sessionStorage.removeItem('learnTech.passwordResetChallenge');
    alert('Senha redefinida com sucesso! Faça login com a sua nova senha.');
    setAuthMode('login');
  } catch (erro) {
    alert(erro.message || 'Erro ao redefinir a senha.');
  } finally {
    esconderCarregamentoAuth();
    if (botao) botao.disabled = false;
  }
}

function restaurarEtapaRedefinicao() {
  const dadosSalvos = sessionStorage.getItem('learnTech.passwordResetChallenge');
  if (!dadosSalvos) return;

  let desafio;
  try {
    desafio = JSON.parse(dadosSalvos);
  } catch {
    sessionStorage.removeItem('learnTech.passwordResetChallenge');
    return;
  }

  if (!desafio
      || typeof desafio.challengeId !== 'string'
      || !Number.isFinite(desafio.expiraEm)
      || desafio.expiraEm <= Date.now()) {
    sessionStorage.removeItem('learnTech.passwordResetChallenge');
    return;
  }

  resetChallengeId = desafio.challengeId;
  resetStep = 'code';
  document.getElementById('reset-email-wrapper').classList.add('hidden');
  document.getElementById('reset-email').required = false;
  document.getElementById('reset-channel-wrapper').classList.add('hidden');
  document.getElementById('reset-codigo-wrapper').classList.remove('hidden');
  document.getElementById('reset-nova-senha-wrapper').classList.remove('hidden');
  document.getElementById('reset-conf-senha-wrapper').classList.remove('hidden');
  document.getElementById('reset-codigo').required = true;
  document.getElementById('reset-nova-senha').required = true;
  document.getElementById('reset-conf-senha').required = true;
  document.getElementById('reset-submit').textContent = 'REDEFINIR SENHA';
  document.getElementById('reset-back-email').classList.add('hidden');
  const status = document.getElementById('reset-status');
  status.textContent = desafio.mensagem || 'Informe o código enviado para o seu e-mail.';
  status.classList.remove('hidden');
  setAuthMode('reset');
}

document.addEventListener('DOMContentLoaded', function () {
  const formLogin = document.getElementById('form-login');
  const loginChannelForm = document.getElementById('form-login-canal');
  const formLoginCodigo = document.getElementById('form-login-codigo');
  const formCadastro = document.getElementById('form-cadastro');
  const formReset = document.getElementById('form-reset');

  if (formLogin) formLogin.addEventListener('submit', fazerLogin);
  if (loginChannelForm) loginChannelForm.addEventListener('submit', enviarCodigoLogin);
  if (formLoginCodigo) formLoginCodigo.addEventListener('submit', verificarCodigoLogin);
  if (formCadastro) formCadastro.addEventListener('submit', fazerCadastro);
  if (formReset) formReset.addEventListener('submit', redefinirSenha);
  restaurarEtapaLogin();
  restaurarEtapaRedefinicao();
});