import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { initializeFirestore, persistentLocalCache, persistentMultipleTabManager } from 'firebase/firestore'

const config = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
}

/** false si falta el archivo .env: la pantalla de login lo avisa en vez de fallar sin explicación. */
export const configurado = Boolean(config.apiKey && config.projectId)

const app = initializeApp(configurado ? config : { apiKey: 'falta', projectId: 'falta', appId: 'falta' })

export const auth = getAuth(app)

// Caché en disco: el panel sigue funcionando sin conexión y sube lo pendiente al volver.
export const db = initializeFirestore(app, {
  localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }),
})
