import { useEffect, useState } from 'react'
import { onAuthStateChanged, type User } from 'firebase/auth'
import { auth } from './firebase'
import { Login } from './Login'
import { Panel } from './Panel'

/** Con sesión de administrador (correo y contraseña) muestra el panel; si no, el login. */
export function App() {
  const [usuario, setUsuario] = useState<User | null | undefined>(undefined)
  useEffect(() => onAuthStateChanged(auth, (u) => setUsuario(u && !u.isAnonymous ? u : null)), [])

  if (usuario === undefined) return null // Firebase todavía está leyendo la sesión guardada
  return usuario ? <Panel usuario={usuario} /> : <Login />
}
