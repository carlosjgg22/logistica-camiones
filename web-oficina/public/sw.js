// Service worker básico: guarda la app (HTML, JS, CSS, fuentes) para abrirla sin conexión.
// Los datos no pasan por aquí: Firestore ya los guarda en el navegador (persistentLocalCache).
const CACHE = 'oficina-v1'

self.addEventListener('install', (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(['/', '/manifest.webmanifest', '/icono.svg'])))
  self.skipWaiting()
})

self.addEventListener('activate', (e) => {
  e.waitUntil(caches.keys().then((claves) => Promise.all(claves.filter((k) => k !== CACHE).map((k) => caches.delete(k)))))
  self.clients.claim()
})

self.addEventListener('fetch', (e) => {
  const req = e.request
  if (req.method !== 'GET') return
  const url = new URL(req.url)
  const propio = url.origin === self.location.origin
  const fuente = url.host === 'fonts.googleapis.com' || url.host === 'fonts.gstatic.com'
  if (!propio && !fuente) return // Firebase y el resto van directo a la red

  // Páginas: primero la red (para tener siempre la última versión), si no hay, la copia guardada
  if (req.mode === 'navigate') {
    e.respondWith(
      fetch(req).then((r) => { const copia = r.clone(); caches.open(CACHE).then((c) => c.put('/', copia)); return r })
        .catch(() => caches.match('/')),
    )
    return
  }
  // Archivos de la app y fuentes: la copia guardada, y se actualiza en segundo plano
  e.respondWith(
    caches.match(req).then((guardada) => {
      const red = fetch(req).then((r) => {
        if (r.ok || r.type === 'opaque') { const copia = r.clone(); caches.open(CACHE).then((c) => c.put(req, copia)) }
        return r
      }).catch(() => guardada)
      return guardada || red
    }),
  )
})
