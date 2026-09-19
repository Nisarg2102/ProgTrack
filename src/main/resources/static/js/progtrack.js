/**
 * progtrack.js — Global JavaScript for ProgTrack
 * Handles: sidebar active state, notification badge polling,
 *          flash message auto-dismiss, tooltips, and PWA install prompt.
 */

document.addEventListener('DOMContentLoaded', function () {

  // ── 1. SIDEBAR ACTIVE STATE ─────────────────────────────────────────────────
  (function markActiveSidebarLink() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('#sidebar-nav .nav-link, #sidebar-nav .nav-content a');

    navLinks.forEach(link => {
      const href = link.getAttribute('href');
      if (href && href !== '#' && currentPath.startsWith(href) && href.length > 1) {
        link.classList.add('active');

        // Expand parent collapse if inside one
        const parent = link.closest('.collapse');
        if (parent) {
          parent.classList.add('show');
          const trigger = document.querySelector('[data-bs-target="#' + parent.id + '"]');
          if (trigger) trigger.classList.remove('collapsed');
        }
      }
    });
  })();

  // ── 2. FLASH MESSAGE AUTO-DISMISS ──────────────────────────────────────────
  (function autoDismissAlerts() {
    const alerts = document.querySelectorAll('.alert.alert-success, .alert.alert-info');
    alerts.forEach(alert => {
      setTimeout(() => {
        const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
        if (bsAlert) bsAlert.close();
      }, 5000);
    });
  })();

  // ── 3. BOOTSTRAP TOOLTIPS ───────────────────────────────────────────────────
  (function initTooltips() {
    const tooltipEls = document.querySelectorAll('[title]');
    tooltipEls.forEach(el => {
      new bootstrap.Tooltip(el, { trigger: 'hover', placement: 'top' });
    });
  })();

  // ── 4. NOTIFICATION BADGE POLLING (every 60s) ────────────────────────────────
  (function pollNotificationCount() {
    const badge = document.querySelector('.badge-number.bg-danger');
    if (!badge) return; // not logged in or no badge in DOM

    function fetchCount() {
      fetch('/notifications/api/count')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
          if (!data) return;
          const count = data.count || 0;
          badge.textContent = count > 0 ? count : '';
          badge.style.display = count > 0 ? '' : 'none';
        })
        .catch(() => {}); // silent fail
    }

    fetchCount();
    setInterval(fetchCount, 60000);
  })();

  // ── 5. CONFIRM DANGEROUS ACTIONS ────────────────────────────────────────────
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', function (e) {
      const msg = this.getAttribute('data-confirm');
      if (!confirm(msg)) e.preventDefault();
    });
  });

  // ── 6. PWA INSTALL PROMPT ───────────────────────────────────────────────────
  let deferredPrompt;
  const installBanner = document.getElementById('pwa-install-banner');

  window.addEventListener('beforeinstallprompt', e => {
    e.preventDefault();
    deferredPrompt = e;
    if (installBanner) installBanner.style.display = 'flex';
  });

  const installBtn = document.getElementById('pwa-install-btn');
  if (installBtn) {
    installBtn.addEventListener('click', async () => {
      if (!deferredPrompt) return;
      deferredPrompt.prompt();
      const { outcome } = await deferredPrompt.userChoice;
      console.log('PWA install outcome:', outcome);
      deferredPrompt = null;
      if (installBanner) installBanner.style.display = 'none';
    });
  }

  const installDismiss = document.getElementById('pwa-dismiss-btn');
  if (installDismiss) {
    installDismiss.addEventListener('click', () => {
      if (installBanner) installBanner.style.display = 'none';
    });
  }

  // ── 7. CHAT UNREAD BADGE ─────────────────────────────────────────────────────
  (function pollChatUnread() {
    const chatBadge = document.querySelector('.nav-icon .badge.bg-success');
    if (!chatBadge) return;

    function fetchUnread() {
      fetch('/chat/api/unread-count')
        .then(r => r.ok ? r.json() : null)
        .then(data => {
          if (!data) return;
          const count = data.count || 0;
          chatBadge.textContent = count > 0 ? count : '';
          chatBadge.style.display = count > 0 ? '' : 'none';
        })
        .catch(() => {});
    }

    fetchUnread();
    setInterval(fetchUnread, 30000);
  })();

  // ── 8. TABLE SORT (data-sort attribute) ──────────────────────────────────────
  document.querySelectorAll('th[data-sort]').forEach(th => {
    th.style.cursor = 'pointer';
    th.addEventListener('click', function () {
      const table = this.closest('table');
      const tbody = table.querySelector('tbody');
      const colIdx = Array.from(this.parentElement.children).indexOf(this);
      const asc = this.getAttribute('data-order') !== 'asc';
      this.setAttribute('data-order', asc ? 'asc' : 'desc');

      const rows = Array.from(tbody.querySelectorAll('tr'));
      rows.sort((a, b) => {
        const aText = a.children[colIdx]?.textContent.trim() || '';
        const bText = b.children[colIdx]?.textContent.trim() || '';
        return asc
          ? aText.localeCompare(bText, undefined, { numeric: true })
          : bText.localeCompare(aText, undefined, { numeric: true });
      });
      rows.forEach(row => tbody.appendChild(row));
    });
  });

  // ── 9. FORM DIRTY CHECK ───────────────────────────────────────────────────────
  let formDirty = false;
  document.querySelectorAll('form').forEach(form => {
    form.addEventListener('change', () => { formDirty = true; });
    form.addEventListener('submit', () => { formDirty = false; });
  });

  window.addEventListener('beforeunload', e => {
    if (formDirty) {
      e.preventDefault();
      e.returnValue = '';
    }
  });

});
