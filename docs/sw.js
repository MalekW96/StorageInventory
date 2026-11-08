/* Service worker: makes the app work offline and lets "Check for updates" install new versions. */
const VERSION = '2.0.0.0';
const CACHE = 'si-' + VERSION;
const MODELS = 'si-models';
const SHELL = [
  './', 'index.html', 'app.js', 'styles.css', 'manifest.webmanifest',
  'vendor/jsQR.js',
  'fonts/geist-regular.woff2', 'fonts/geist-medium.woff2', 'fonts/geist-semibold.woff2', 'fonts/geist-bold.woff2',
  'icons/apple-touch-icon.png', 'icons/icon-192.png', 'icons/icon-512.png',
];

self.addEventListener('install', (e) => {
  // cache: 'reload' skips the browser's HTTP cache so an update never installs stale files.
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL.map((u) => new Request(u, { cache: 'reload' })))));
});

self.addEventListener('activate', (e) => {
  e.waitUntil((async () => {
    for (const key of await caches.keys()) {
      if (key.startsWith('si-') && key !== CACHE && key !== MODELS) await caches.delete(key);
    }
    await self.clients.claim();
  })());
});

self.addEventListener('message', (e) => { if (e.data === 'skipWaiting') self.skipWaiting(); });

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // Always ask the server for the latest version info.
  if (url.pathname.endsWith('/version.json')) return;

  // AI model files (TensorFlow.js + MobileNet weights): cache after first download.
  const isModel = url.pathname.includes('/vendor/tf') || url.pathname.includes('/vendor/mobilenet') ||
    /tfhub\.dev|kaggle|storage\.googleapis\.com/.test(url.hostname);
  if (isModel) {
    e.respondWith(caches.open(MODELS).then(async (c) => {
      const hit = await c.match(req);
      if (hit) return hit;
      const res = await fetch(req);
      if (res.ok || res.type === 'opaque') c.put(req, res.clone());
      return res;
    }));
    return;
  }

  if (url.origin !== self.location.origin) return;
  e.respondWith(caches.match(req, { ignoreSearch: true }).then((hit) => hit || fetch(req)));
});
