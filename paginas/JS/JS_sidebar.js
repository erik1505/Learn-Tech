/* =========================================================
   LEARN TECH - MENU LATERAL (SIDEBAR)
   Usado em: dashboard, turmas, atividades e calendário.

   - Telas grandes: o botão "<" recolhe/expande o menu.
   - Telas pequenas (até 768px): o menu vira uma gaveta que abre
     pelo botão hambúrguer e fecha ao clicar no fundo escuro.
   ========================================================= */

document.addEventListener('DOMContentLoaded', function () {
  const sidebar = document.getElementById('main-sidebar');
  const toggleBtn = document.getElementById('sidebar-toggle');
  const mobileTrigger = document.getElementById('mobile-menu-trigger');
  const backdrop = document.getElementById('sidebar-backdrop');

  if (!sidebar) {
    return;
  }

  function openMobileMenu() {
    sidebar.classList.add('mobile-open');

    if (backdrop) {
      backdrop.classList.add('active');
    }
  }

  function closeMobileMenu() {
    sidebar.classList.remove('mobile-open');

    if (backdrop) {
      backdrop.classList.remove('active');
    }
  }

  // Recolhe/expande a sidebar no desktop
  if (toggleBtn) {
    toggleBtn.addEventListener('click', function () {
      sidebar.classList.toggle('collapsed');
      toggleBtn.innerHTML = sidebar.classList.contains('collapsed') ? '&gt;' : '&lt;';
    });
  }

  // Abre/fecha a gaveta no celular
  if (mobileTrigger) {
    mobileTrigger.addEventListener('click', function (event) {
      event.stopPropagation();

      if (sidebar.classList.contains('mobile-open')) {
        closeMobileMenu();
      } else {
        openMobileMenu();
      }
    });
  }

  // Fecha a gaveta ao clicar no fundo escurecido
  if (backdrop) {
    backdrop.addEventListener('click', closeMobileMenu);
  }

  // Fecha a gaveta se a tela voltar a ser grande
  window.addEventListener('resize', function () {
    if (window.innerWidth > 768) {
      closeMobileMenu();
    }
  });
});
