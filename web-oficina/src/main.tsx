import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './estilos.css'

createRoot(document.getElementById('raiz')!).render(<StrictMode><App /></StrictMode>)

// PWA: el service worker solo en la versión publicada (en desarrollo molestaría con la caché)
if ('serviceWorker' in navigator && import.meta.env.PROD) {
  window.addEventListener('load', () => { navigator.serviceWorker.register('/sw.js').catch(() => {}) })
}
