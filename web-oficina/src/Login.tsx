import { useState, type FormEvent } from 'react'
import {
  browserLocalPersistence, browserSessionPersistence, sendPasswordResetEmail, setPersistence, signInWithEmailAndPassword,
} from 'firebase/auth'
import { auth, configurado } from './firebase'
import { Icono, MensajeError } from './componentes/base'

/** Mismos mensajes que FlotaRepository.entrarAdministrador en la app. */
function mensaje(codigo: string | undefined): string {
  switch (codigo) {
    case 'auth/invalid-credential':
    case 'auth/invalid-email':
    case 'auth/user-not-found':
    case 'auth/wrong-password':
    case 'auth/user-disabled':
      return 'Correo o contraseña incorrectos.'
    case 'auth/network-request-failed':
      return 'Sin conexión. Revisa la señal e intenta de nuevo.'
    case 'auth/too-many-requests':
      return 'Demasiados intentos. Espera unos minutos e intenta de nuevo.'
    default:
      return 'No se pudo iniciar sesión. Intenta de nuevo.'
  }
}

export function Login() {
  const [correo, setCorreo] = useState('')
  const [clave, setClave] = useState('')
  const [verClave, setVerClave] = useState(false)
  const [mantener, setMantener] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const entrar = async (e: FormEvent) => {
    e.preventDefault()
    if (cargando) return
    setInfo(null)
    if (!correo.trim() || !clave) { setError('Escribe tu correo y tu contraseña.'); return }
    setCargando(true)
    try {
      // Sin "Mantener la sesión iniciada" la sesión dura hasta cerrar el navegador
      await setPersistence(auth, mantener ? browserLocalPersistence : browserSessionPersistence)
      await signInWithEmailAndPassword(auth, correo.trim(), clave)
    } catch (err) {
      setError(mensaje((err as { code?: string }).code))
      setCargando(false)
    }
  }

  const recuperar = async () => {
    setInfo(null)
    if (!correo.trim()) { setError('Escribe tu correo y te mandamos un enlace para cambiar la contraseña.'); return }
    try {
      await sendPasswordResetEmail(auth, correo.trim())
      setError(null)
      setInfo(`Si ${correo.trim()} tiene usuario, le llegará un correo para cambiar la contraseña.`)
    } catch (err) {
      setError(mensaje((err as { code?: string }).code))
    }
  }

  return (
    <div className="login">
      <section className="login-marca">
        <div className="logo">
          <span className="logo-circulo"><Icono nombre="local_shipping" relleno /></span>
          LogisticaGarcía
        </div>
        <div>
          <h1>Oficina de despachos</h1>
          <p>Programa viajes, arma unidades y sigue en vivo la salida, llegada y entrega de cada operador.</p>
          <span className="nota"><Icono nombre="sync" tam={18} />Mismos datos que la app de Android</span>
        </div>
      </section>

      <form className="login-form" onSubmit={entrar} noValidate>
        <h2>Iniciar sesión</h2>
        <p className="sub">Acceso para administradores de la oficina.</p>

        {!configurado && (
          <MensajeError texto="Falta la configuración de Firebase: copia .env.example como .env y complétalo (ver README)." />
        )}

        <div className="campo">
          <label htmlFor="correo">Correo</label>
          <div className={'entrada' + (error ? ' error' : '')}>
            <input
              id="correo" type="email" autoComplete="username" placeholder="admin@logisticagarcia.com"
              value={correo} onChange={(e) => { setCorreo(e.target.value); setError(null) }}
            />
          </div>
        </div>
        <div className="campo">
          <label htmlFor="clave">Contraseña</label>
          <div className={'entrada' + (error ? ' error' : '')}>
            <input
              id="clave" type={verClave ? 'text' : 'password'} autoComplete="current-password"
              value={clave} onChange={(e) => { setClave(e.target.value); setError(null) }}
            />
            <button
              type="button" className="boton-icono" onClick={() => setVerClave(!verClave)}
              aria-label={verClave ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            >
              <Icono nombre={verClave ? 'visibility_off' : 'visibility'} />
            </button>
          </div>
        </div>

        <div className="fila-entre">
          <button type="button" className="check" onClick={() => setMantener(!mantener)} aria-pressed={mantener}>
            <Icono nombre={mantener ? 'check_box' : 'check_box_outline_blank'} relleno={mantener} />
            Mantener la sesión iniciada
          </button>
          <button type="button" className="link" onClick={recuperar}>¿Olvidaste tu contraseña?</button>
        </div>

        {error && <MensajeError texto={error} />}
        {info && <div className="aviso"><Icono nombre="info" />{info}</div>}

        <button type="submit" className="boton boton-grande" disabled={cargando || !configurado}>
          <Icono nombre="login" relleno />{cargando ? 'Entrando…' : 'Entrar'}
        </button>
        <p className="pie">Los operadores entran desde la app de Android con su cédula.</p>
      </form>
    </div>
  )
}
