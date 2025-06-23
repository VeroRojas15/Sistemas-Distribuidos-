const CACHE_NAME = 'agenda-cache-v4';
const FILES_TO_CACHE = [
  './',
  './index.html',
  './style.css',
  './app.js',
  './manifest.json',
  './offline.html',
  './img/192x192.png',
  './img/512x512.png'
];

// Instalar el service worker y cachear archivos
self.addEventListener('install', event => {
  console.log('[Service Worker] Instalando última versión...');
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(FILES_TO_CACHE);
    })
  );
  self.skipWaiting(); // Forzar que el nuevo SW se active de inmediato
});

// Activar y limpiar cachés viejas
self.addEventListener('activate', event => {
  console.log('[Service Worker] Activado');
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(
        keys.map(key => {
          if (key !== CACHE_NAME) {
            console.log('[Service Worker] Borrando caché vieja:', key);
            return caches.delete(key);
          }
        })
      );
    })
  );
  return self.clients.claim(); // tomar control de las páginas abiertas
});

// Interceptar peticiones y responder desde caché si es posible
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(response => {
      return response || fetch(event.request).catch(() => {
        // Solo servir offline.html si el request es navegación
        if (event.request.mode === 'navigate') {
          return caches.match('./offline.html');
        }
      });
    })
  );
});


self.addEventListener('message', event => {
  if (event.data.action === 'skipWaiting') {
    self.skipWaiting();
  }
});

