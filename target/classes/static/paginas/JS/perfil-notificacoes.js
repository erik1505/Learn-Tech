/* =========================================================
   LEARN TECH - PERFIL E NOTIFICAÇÕES
   Usado em todas as páginas internas que têm o sino e o ícone
   de perfil no header.

   O HTML do painel de perfil e do modal "Editar perfil" fica
   em cada página. O painel de notificações é criado aqui.
   ========================================================= */


/* =========================================================
   FUNÇÕES AUXILIARES
   ========================================================= */

function getElement(id) {
  return document.getElementById(id);
}

// Abre ou fecha um painel e atualiza os atributos de acessibilidade.
// O botão que abre o painel é opcional (o modal de edição não tem botão).
function setPanelOpen(panelId, buttonId, isOpen) {
  const panel = getElement(panelId);
  const button = buttonId ? getElement(buttonId) : null;

  if (panel) {
    panel.classList.toggle('active', isOpen);
    panel.setAttribute('aria-hidden', isOpen ? 'false' : 'true');
  }

  if (button) {
    button.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
  }
}


/* =========================================================
   NOTIFICAÇÕES - DADOS
   (carregadas do back-end por carregarNotificacoes)
   ========================================================= */

let notificationsData = [];
const DEMO_NOTIFICATIONS_READ_KEY = 'learntech_notificacoes_demo_lidas';

function carregarNotificacoesDemo() {
  const lidas = new Set(JSON.parse(localStorage.getItem(DEMO_NOTIFICATIONS_READ_KEY) || '[]'));
  const notificacoes = [
    { id: 'demo-notificacao-1', icon: '📅', title: 'Atividade próxima do prazo', message: 'Revisão de funções vence amanhã.', time: 'Agora' },
    { id: 'demo-notificacao-2', icon: '✅', title: 'Atividade concluída', message: 'Redação: tecnologia e sociedade foi marcada como concluída.', time: 'Há 1 h' },
    { id: 'demo-notificacao-3', icon: '🧪', title: 'Modo de demonstração', message: 'Você está vendo notificações fictícias; nenhuma foi enviada pelo servidor.', time: 'Hoje' }
  ];

  notificationsData = notificacoes.map(function (notification) {
    return Object.assign({}, notification, { unread: !lidas.has(notification.id) });
  });
  renderNotifications();
}

function salvarNotificacoesDemoLidas() {
  const lidas = notificationsData
    .filter(function (notification) { return !notification.unread; })
    .map(function (notification) { return notification.id; });
  localStorage.setItem(DEMO_NOTIFICATIONS_READ_KEY, JSON.stringify(lidas));
}


/* =========================================================
   NOTIFICAÇÕES - CRIAÇÃO E LISTA
   ========================================================= */

// Cria o painel de notificações e coloca no final do <body>
function createNotificationsPanel() {
  if (getElement('notifications-panel')) {
    return;
  }

  const panel = document.createElement('aside');
  panel.id = 'notifications-panel';
  panel.className = 'profile-notification-panel notifications-panel';
  panel.setAttribute('aria-hidden', 'true');

  panel.innerHTML = `
    <div class="notifications-header">
      <div>
        <span class="panel-eyebrow">CENTRAL</span>
        <h2>Notificações</h2>
      </div>

      <button
        type="button"
        class="panel-close-btn"
        id="notifications-close-button"
        title="Fechar"
        aria-label="Fechar notificações"
      >×</button>
    </div>

    <div class="notifications-actions">
      <button type="button" id="mark-all-notifications">Marcar todas como lidas</button>
    </div>

    <div id="notifications-list" class="notifications-list"></div>
  `;

  document.body.appendChild(panel);

  getElement('notifications-close-button').addEventListener('click', closeNotifications);
  getElement('mark-all-notifications').addEventListener('click', markAllNotificationsAsRead);

  renderNotifications();
}

function createProfilePanel() {
  if (!getElement('profile-panel')) {
    const panel = document.createElement('aside');
    panel.id = 'profile-panel';
    panel.className = 'profile-notification-panel profile-panel';
    panel.setAttribute('aria-hidden', 'true');
    panel.innerHTML = `
      <div class="profile-panel-header">
        <div><span class="panel-eyebrow">MINHA CONTA</span><h2>Perfil</h2></div>
        <button type="button" class="panel-close-btn" onclick="closeProfile()" title="Fechar" aria-label="Fechar perfil">×</button>
      </div>
      <div class="profile-summary">
        <div class="profile-avatar" aria-hidden="true"></div>
        <div class="profile-summary-text">
          <strong id="profile-name"></strong>
          <span id="profile-email"></span>
        </div>
      </div>
      <div class="profile-info-list">
        <div class="profile-info-row"><span>NOME</span><strong id="profile-name-info"></strong></div>
        <div class="profile-info-row"><span>E-MAIL</span><strong id="profile-email-info"></strong></div>
        <div class="profile-info-row"><span>TIPO DE CONTA</span><strong></strong></div>
      </div>
      <div class="profile-actions">
        <button type="button" class="profile-action-primary" onclick="openEditProfile()">EDITAR PERFIL</button>
        <button type="button" class="profile-action-secondary" onclick="logoutUser()">SAIR DA CONTA</button>
      </div>
    `;
    document.body.appendChild(panel);
  }

  if (!getElement('profile-edit-modal')) {
    const modal = document.createElement('div');
    modal.id = 'profile-edit-modal';
    modal.className = 'profile-edit-overlay';
    modal.setAttribute('aria-hidden', 'true');
    modal.innerHTML = `
      <div class="profile-edit-modal" role="dialog" aria-modal="true" aria-labelledby="profile-edit-title">
        <div class="profile-edit-header">
          <div><span class="panel-eyebrow">MINHA CONTA</span><h2 id="profile-edit-title">Editar perfil</h2></div>
          <button type="button" class="panel-close-btn" onclick="closeEditProfile()" title="Fechar" aria-label="Fechar edição">×</button>
        </div>
        <div class="profile-edit-form">
          <label for="profile-name-input">NOME</label>
          <input id="profile-name-input" type="text" autocomplete="name">
          <label for="profile-email-input">E-MAIL</label>
          <input id="profile-email-input" type="email" autocomplete="email">
          <div class="profile-edit-buttons">
            <button type="button" class="profile-action-secondary" onclick="closeEditProfile()">CANCELAR</button>
            <button type="button" class="profile-action-primary" onclick="saveProfile()">SALVAR</button>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(modal);
  }
}

// Desenha a lista de notificações (ou a mensagem de lista vazia)
function renderNotifications() {
  const list = getElement('notifications-list');

  if (!list) {
    return;
  }

  list.innerHTML = '';

  if (notificationsData.length === 0) {
    list.innerHTML = `
      <div class="notifications-empty">
        <div class="notifications-empty-icon">🔔</div>
        <strong>Tudo em dia</strong>
        <span>Você não possui novas notificações.</span>
      </div>
    `;

    updateNotificationBadge();
    return;
  }

  notificationsData.forEach(function (notification) {
    const item = document.createElement('button');
    item.type = 'button';
    item.className = 'notification-item';
    item.dataset.id = notification.id;

    if (notification.unread) {
      item.classList.add('unread');
    }

    item.innerHTML = `
      <span class="notification-icon">${esc(notification.icon)}</span>

      <span class="notification-content">
        <strong>${esc(notification.title)}</strong>
        <span class="notification-message">${esc(notification.message)}</span>
        <span class="notification-time">${esc(notification.time)}</span>
      </span>

      <span class="notification-dot" aria-hidden="true"></span>
    `;

    item.addEventListener('click', function () {
      markNotificationAsRead(notification.id);
    });

    list.appendChild(item);
  });

  updateNotificationBadge();
}

function addNotification(notification) {
  notificationsData.unshift({
    id: notification.id || Date.now(),
    icon: notification.icon || '🔔',
    title: notification.title || 'Nova notificação',
    message: notification.message || '',
    time: notification.time || 'Agora',
    unread: notification.unread !== false
  });

  renderNotifications();
}

async function markNotificationAsRead(id) {
  const notification = notificationsData.find(function (item) {
    return item.id === id;
  });

  if (!notification || !notification.unread) {
    return;
  }

  notification.unread = false;
  renderNotifications();

  if (typeof modoDemonstracaoAtivo === 'function' && modoDemonstracaoAtivo()) {
    try {
      salvarNotificacoesDemoLidas();
    } catch (erro) {
      notification.unread = true;
      renderNotifications();
      alert('Não foi possível salvar a notificação como lida neste navegador.');
    }
    return;
  }

  try {
    await marcarLida(id, usuarioLogado().id);
  } catch (erro) {
    notification.unread = true;
    renderNotifications();
    alert(erro.message || 'Não foi possível marcar a notificação como lida.');
  }
}

async function markAllNotificationsAsRead() {
  const anteriores = notificationsData.map(function (n) { return n.unread; });

  notificationsData.forEach(function (notification) {
    notification.unread = false;
  });

  renderNotifications();

  if (typeof modoDemonstracaoAtivo === 'function' && modoDemonstracaoAtivo()) {
    try {
      salvarNotificacoesDemoLidas();
    } catch (erro) {
      notificationsData.forEach(function (n, i) { n.unread = anteriores[i]; });
      renderNotifications();
      alert('Não foi possível salvar as notificações neste navegador.');
    }
    return;
  }

  try {
    await marcarTodasLidas(usuarioLogado().id);
  } catch (erro) {
    notificationsData.forEach(function (n, i) { n.unread = anteriores[i]; });
    renderNotifications();
    alert(erro.message || 'Não foi possível marcar todas as notificações como lidas.');
  }
}

// Mostra no sino a quantidade de notificações não lidas
function updateNotificationBadge() {
  const badge = getElement('notification-badge');

  if (!badge) {
    return;
  }

  const unread = notificationsData.filter(function (notification) {
    return notification.unread;
  }).length;

  if (unread === 0) {
    badge.classList.add('hidden');
    badge.textContent = '';
    return;
  }

  badge.classList.remove('hidden');
  badge.textContent = unread > 99 ? '99+' : unread;
}


/* =========================================================
   NOTIFICAÇÕES - ABRIR, FECHAR E POSICIONAR
   ========================================================= */

function openNotifications() {
  if (!getElement('notifications-panel')) {
    return;
  }

  carregarNotificacoes();

  // Só um painel aberto por vez: fecha o perfil
  setPanelOpen('profile-panel', 'profile-button', false);

  setPanelOpen('notifications-panel', 'notification-button', true);
  positionNotifications();
}

function closeNotifications() {
  setPanelOpen('notifications-panel', 'notification-button', false);
}

function toggleNotifications() {
  const panel = getElement('notifications-panel');

  if (!panel) {
    return;
  }

  if (panel.classList.contains('active')) {
    closeNotifications();
  } else {
    openNotifications();
  }
}

// Coloca o painel logo abaixo do sino, sem sair da tela
function positionNotifications() {
  const panel = getElement('notifications-panel');
  const button = getElement('notification-button');

  if (!panel || !button) {
    return;
  }

  // No celular a posição é definida pelo CSS
  if (window.innerWidth <= 600) {
    panel.style.top = '';
    panel.style.left = '';
    panel.style.right = '';
    return;
  }

  const buttonRect = button.getBoundingClientRect();
  const panelWidth = 380;
  const margin = 15;

  let left = buttonRect.right - panelWidth;

  if (left < margin) {
    left = margin;
  }

  if (left + panelWidth > window.innerWidth - margin) {
    left = window.innerWidth - panelWidth - margin;
  }

  panel.style.left = `${left}px`;
  panel.style.right = 'auto';
  panel.style.top = `${buttonRect.bottom + 10}px`;
}


/* =========================================================
   PERFIL
   ========================================================= */

function toggleProfile() {
  const panel = getElement('profile-panel');

  if (!panel) {
    return;
  }

  const willOpen = !panel.classList.contains('active');

  // Só um painel aberto por vez: fecha as notificações
  closeNotifications();

  setPanelOpen('profile-panel', 'profile-button', willOpen);
}

function closeProfile() {
  setPanelOpen('profile-panel', 'profile-button', false);
}

function openEditProfile() {
  closeProfile();

  if (!getElement('profile-edit-modal')) {
    return;
  }

  setPanelOpen('profile-edit-modal', null, true);

  const nameInput = getElement('profile-name-input');

  if (nameInput) {
    nameInput.focus();
    nameInput.select();
  }
}

function closeEditProfile() {
  setPanelOpen('profile-edit-modal', null, false);
}

async function saveProfile() {
  const nameInput = getElement('profile-name-input');
  const emailInput = getElement('profile-email-input');

  if (!nameInput || !emailInput) {
    return;
  }

  const newName = nameInput.value.trim();
  const newEmail = emailInput.value.trim();

  if (newName === '') {
    alert('Digite seu nome.');
    nameInput.focus();
    return;
  }

  if (newEmail === '') {
    alert('Digite seu e-mail.');
    emailInput.focus();
    return;
  }

  try {
    const usuario = usuarioLogado();
    const atualizado = modoDemonstracaoAtivo()
      ? { nome: newName, email: newEmail }
      : await atualizarPerfil(usuario.usuarioId || usuario.id, newName, newEmail);

    salvarUsuario(Object.assign({}, usuario, { nome: atualizado.nome, email: atualizado.email }));
    preencherPerfil();
    closeEditProfile();
  } catch (erro) {
    alert(erro.message);
  }
}

async function logoutUser() {
  const confirmLogout = confirm('Deseja realmente sair da sua conta?');

  if (!confirmLogout) {
    return;
  }

  try {
    await sair();
  } catch (erro) {
    console.error('A sessão será encerrada neste dispositivo, mas não foi possível invalidá-la no servidor.', erro);
  }
  window.location.href = 'autenticacao.html';
}

// Preenche o painel de perfil com os dados do usuário logado
function preencherPerfil() {
  const usuario = usuarioLogado();

  if (!usuario) {
    return;
  }

  const iniciais = usuario.nome.trim().split(/\s+/).slice(0, 2).map(function (parte) {
    return parte.charAt(0);
  }).join('').toUpperCase();

  function definir(id, valor) {
    const el = getElement(id);

    if (el) {
      el.textContent = valor;
    }
  }

  definir('profile-name', usuario.nome);
  definir('profile-name-info', usuario.nome);
  definir('profile-email', usuario.email);
  definir('profile-email-info', usuario.email);

  const avatar = document.querySelector('.profile-avatar');

  if (avatar) {
    avatar.textContent = iniciais;
  }

  const tipoConta = document.querySelector('#profile-panel .profile-info-row:last-child strong');

  if (tipoConta) {
    tipoConta.textContent = usuario.role === 'PROFESSOR' ? 'DOCENTE' : 'ALUNO';
  }

  const nameInput = getElement('profile-name-input');
  const emailInput = getElement('profile-email-input');

  if (nameInput) {
    nameInput.value = usuario.nome;
  }

  if (emailInput) {
    emailInput.value = usuario.email;
  }
}


/* =========================================================
   NOTIFICAÇÕES - CARREGAR DO BACK-END
   ========================================================= */

function tempoRelativo(iso) {
  const segundos = Math.max(0, (Date.now() - new Date(iso).getTime()) / 1000);

  if (segundos < 60) {
    return 'Agora';
  }

  const minutos = Math.floor(segundos / 60);

  if (minutos < 60) {
    return 'Há ' + minutos + ' min';
  }

  const horas = Math.floor(minutos / 60);

  if (horas < 24) {
    return 'Há ' + horas + ' h';
  }

  const dias = Math.floor(horas / 24);

  return dias === 1 ? 'Ontem' : 'Há ' + dias + ' dias';
}

async function carregarNotificacoes() {
  const usuario = usuarioLogado();

  if (!usuario) {
    return;
  }

  if (usuario.modoDemonstracao) {
    try {
      carregarNotificacoesDemo();
    } catch (erro) {
      console.warn('Notificações de demonstração:', erro.message);
      notificationsData = [];
      renderNotifications();
    }
    return;
  }

  try {
    const lista = await notificacoes(usuario.id);

    notificationsData = lista.map(function (n) {
      return {
        id: n.id,
        icon: n.icone || '🔔',
        title: n.titulo,
        message: n.mensagem || '',
        time: tempoRelativo(n.criadaEm),
        unread: !n.lida
      };
    });

    renderNotifications();
  } catch (erro) {
    console.warn('Notificações:', erro.message);
  }
}


/* =========================================================
   EVENTOS
   ========================================================= */

// Clicar fora do painel de notificações fecha o painel
document.addEventListener('click', function (event) {
  const panel = getElement('notifications-panel');
  const button = getElement('notification-button');

  if (!panel || !panel.classList.contains('active')) {
    return;
  }

  const clickedInsidePanel = panel.contains(event.target);
  const clickedOnButton = button && button.contains(event.target);

  if (!clickedInsidePanel && !clickedOnButton) {
    closeNotifications();
  }
});

// Esc fecha tudo
document.addEventListener('keydown', function (event) {
  if (event.key === 'Escape') {
    closeNotifications();
    closeProfile();
    closeEditProfile();
  }
});

// Ao redimensionar a tela, reposiciona o painel de notificações aberto
window.addEventListener('resize', function () {
  const panel = getElement('notifications-panel');

  if (panel && panel.classList.contains('active')) {
    positionNotifications();
  }
});

// Início: cria o painel e liga o clique do sino
document.addEventListener('DOMContentLoaded', function () {
  createProfilePanel();
  createNotificationsPanel();

  const profileButton = getElement('profile-button');
  if (profileButton) {
    profileButton.setAttribute('aria-controls', 'profile-panel');
    profileButton.setAttribute('aria-expanded', 'false');
  }

  const notificationButton = getElement('notification-button');

  if (notificationButton) {
    notificationButton.setAttribute('aria-controls', 'notifications-panel');
    notificationButton.setAttribute('aria-expanded', 'false');
    if (!getElement('notification-badge')) {
      const badge = document.createElement('span');
      badge.id = 'notification-badge';
      badge.className = 'notification-badge hidden';
      notificationButton.appendChild(badge);
    }
    notificationButton.addEventListener('click', function (event) {
      event.stopPropagation();
      toggleNotifications();
    });
  }

  updateNotificationBadge();

  // Dados reais: perfil, notificações e botão "Sair" do menu lateral
  preencherPerfil();
  carregarNotificacoes();
  setInterval(carregarNotificacoes, 60000);

  document.querySelectorAll('.btn-sair').forEach(function (botao) {
    botao.addEventListener('click', logoutUser);
  });
});
