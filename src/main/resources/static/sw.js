/**
 * ProgTrack Service Worker
 * Implements Cache-First for static assets, Network-First for API/pages.
 */

const CACHE_NAME      = 'progtrack-v1';
const OFFLINE_URL     = '/login';

// Static assets to pre-cache on install
const PRECACHE_URLS = [
  '/',
  '/login',
  '/css/progtrack.css',
  '/js/progtrack.js',
  '/manifest.json',
  '/assets/vendor/bootstrap/css/bootstrap.min.css',
  '/assets/vendor/bootstrap-icons/bootstrap-icons.css',
  '/assets/vendor/bootstrap/js/bootstrap.bundle.min.js',
  '/assets/vendor/chart.js/chart.umd.js',
];

// ── INSTALL ──────────────────────────────────────────────────────────────────
self.addEventListener('install', event => {
  console.log('[SW] Installing ProgTrack Service Worker…');
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('[SW] Pre-caching static assets');
        return cache.addAll(PRECACHE_URLS.filter(url => {
          // Only cache URLs that are likely to succeed
          return !url.includes('undefined');
        }));
      })
      .then(() => self.skipWaiting())
      .catch(err => {
        console.warn('[SW] Pre-cache error (non-fatal):', err.message);
        return self.skipWaiting();
      })
  );
});

// ── ACTIVATE ─────────────────────────────────────────────────────────────────
self.addEventListener('activate', event => {
  console.log('[SW] Activating ProgTrack Service Worker…');
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.filter(key => key !== CACHE_NAME).map(key => {
          console.log('[SW] Deleting old cache:', key);
          return caches.delete(key);
        })
      );
    }).then(() => self.clients.claim())
  );
});

// ── FETCH ────────────────────────────────────────────────────────────────────
self.addEventListener('fetch', event => {
  const { request } = event;
  const url = new URL(request.url);

  // Skip non-GET, non-same-origin, WebSocket, and API requests
  if (
    request.method !== 'GET' ||
    url.origin !== location.origin ||
    url.pathname.startsWith('/ws') ||
    url.pathname.startsWith('/api/') ||
    url.pathname.startsWith('/app/') ||
    url.pathname.startsWith('/topic/')
  ) {
    return; // Let the browser handle it
  }

  // Static assets: Cache-First
  const isStaticAsset = url.pathname.match(/\.(css|js|png|jpg|jpeg|gif|svg|ico|woff|woff2|ttf)$/);
  if (isStaticAsset) {
    event.respondWith(
      caches.match(request).then(cached => {
        if (cached) return cached;
        return fetch(request).then(response => {
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(request, clone));
          }
          return response;
        });
      }).catch(() => new Response('Asset not available offline', { status: 503 }))
    );
    return;
  }

  // HTML pages: Network-First with offline fallback
  if (request.headers.get('accept')?.includes('text/html')) {
    event.respondWith(
      fetch(request)
        .then(response => {
          // Cache successful HTML responses
          if (response.ok) {
            const clone = response.clone();
            caches.open(CACHE_NAME).then(cache => cache.put(request, clone));
          }
          return response;
        })
        .catch(() => {
          // Offline: try cache, then fallback to offline page
          return caches.match(request)
            .then(cached => cached || caches.match(OFFLINE_URL))
            .then(response => response || new Response(
              '<h1>ProgTrack — Offline</h1><p>Please check your internet connection.</p>',
              { headers: { 'Content-Type': 'text/html' } }
            ));
        })
    );
    return;
  }
});

// ── PUSH NOTIFICATIONS (future) ───────────────────────────────────────────────
self.addEventListener('push', event => {
  const data = event.data?.json() || {};
  const options = {
    body:    data.body    || 'New notification from ProgTrack',
    icon:    data.icon    || '/assets/img/icons/icon-192x192.png',
    badge:   data.badge   || '/assets/img/icons/icon-72x72.png',
    data:    { url: data.url || '/' },
    actions: [{ action: 'view', title: 'View' }]
  };
  event.waitUntil(
    self.registration.showNotification(data.title || 'ProgTrack', options)
  );
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window' }).then(clientList => {
      const url = event.notification.data?.url || '/';
      for (const client of clientList) {
        if (client.url === url && 'focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow(url);
    })
  );
});
