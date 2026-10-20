import { useEffect, useId, useState, type ReactNode } from 'react'
import { ETIQUETA_ESTADO, type EstadoViaje } from '../tipos'
import { aInputFechaHora, deInputFechaHora, duracion } from '../formato'

// ---------- Íconos y chips ----------

export function Icono({ nombre, relleno = false, tam }: { nombre: string; relleno?: boolean; tam?: number }) {
  return (
    <span className={'icono' + (relleno ? ' relleno' : '')} style={tam ? { fontSize: tam } : undefined} aria-hidden>
      {nombre}
    </span>
  )
}

const COLOR_ESTADO: Record<EstadoViaje, string> = {
  SIN_PROGRAMAR: 'var(--estado-sin-programar)',
  PROGRAMADO: 'var(--estado-programado)',
  EN_RUTA: 'var(--estado-en-ruta)',
  COMPLETADO: 'var(--estado-completado)',
}

/** Fondo = el color al 14% de alfa, como ChipTexto en la app. */
export function ChipTexto({ texto, color }: { texto: string; color: string }) {
  return (
    <span className="chip" style={{ color, background: `color-mix(in srgb, ${color} 14%, transparent)` }}>
      {texto}
    </span>
  )
}

export const ChipEstado = ({ estado }: { estado: EstadoViaje }) =>
  <ChipTexto texto={ETIQUETA_ESTADO[estado]} color={COLOR_ESTADO[estado]} />

export const ChipLibre = ({ ocupado, textoOcupado, textoLibre = 'Libre' }: { ocupado: boolean; textoOcupado: string; textoLibre?: string }) =>
  ocupado
    ? <ChipTexto texto={textoOcupado} color="var(--estado-en-ruta)" />
    : <ChipTexto texto={textoLibre} color="var(--estado-completado)" />

// ---------- Avisos ----------

export const MensajeError = ({ texto }: { texto: string }) =>
  <div className="aviso error" role="alert"><Icono nombre="error" relleno />{texto}</div>

export const AvisoInfo = ({ texto }: { texto: string }) =>
  <div className="aviso"><Icono nombre="info" />{texto}</div>

export function EstadoVacio({ icono, titulo, texto }: { icono: string; titulo: string; texto: string }) {
  return <div className="vacio"><Icono nombre={icono} /><b>{titulo}</b><span>{texto}</span></div>
}

/** Mensaje abajo al centro que se va solo. */
export function Snackbar({ texto, onFin }: { texto: string | null; onFin: () => void }) {
  useEffect(() => {
    if (!texto) return
    const t = setTimeout(onFin, 4000)
    return () => clearTimeout(t)
  }, [texto, onFin])
  return texto ? <div className="snackbar" role="status">{texto}</div> : null
}

// ---------- Campos de formulario ----------

export function Campo({ etiqueta, ayuda, alerta, children }: { etiqueta: string; ayuda?: string | null; alerta?: boolean; children: (id: string) => ReactNode }) {
  const id = useId()
  return (
    <div className="campo">
      <label htmlFor={id}>{etiqueta}</label>
      {children(id)}
      {ayuda && <span className={'ayuda' + (alerta ? ' alerta' : '')}>{ayuda}</span>}
    </div>
  )
}

export function CampoTexto({ etiqueta, valor, onCambio, numerico, sufijo }: {
  etiqueta: string; valor: string; onCambio: (v: string) => void; numerico?: boolean; sufijo?: string
}) {
  return (
    <Campo etiqueta={etiqueta}>
      {(id) => (
        <div className="entrada">
          <input
            id={id} value={valor} inputMode={numerico ? 'numeric' : undefined}
            onChange={(e) => onCambio(numerico ? e.target.value.replace(/\D/g, '') : e.target.value)}
          />
          {sufijo && <span className="ayuda">{sufijo}</span>}
        </div>
      )}
    </Campo>
  )
}

/** Lista desplegable. [vacio] agrega una primera opción sin valor (por ejemplo "Sin asignar"). */
export function Selector<T>({ etiqueta, opciones, valor, clave, texto, onCambio, vacio, ayuda, alerta }: {
  etiqueta: string; opciones: T[]; valor: string | null; clave: (x: T) => string; texto: (x: T) => string
  onCambio: (id: string | null) => void; vacio?: string; ayuda?: string | null; alerta?: boolean
}) {
  return (
    <Campo etiqueta={etiqueta} ayuda={ayuda} alerta={alerta}>
      {(id) => (
        <div className="entrada">
          <select id={id} value={valor ?? ''} onChange={(e) => onCambio(e.target.value || null)}>
            <option value="" disabled={!vacio}>{vacio ?? 'Elegir…'}</option>
            {opciones.map((o) => <option key={clave(o)} value={clave(o)}>{texto(o)}</option>)}
          </select>
        </div>
      )}
    </Campo>
  )
}

/** Fecha y hora opcionales (salida, llegada, entrega), con botón para quitarla. */
export function CampoFechaHora({ etiqueta, valor, onCambio }: { etiqueta: string; valor: number | null; onCambio: (v: number | null) => void }) {
  return (
    <Campo etiqueta={etiqueta}>
      {(id) => (
        <div className="entrada">
          <input id={id} type="datetime-local" value={aInputFechaHora(valor)} onChange={(e) => onCambio(deInputFechaHora(e.target.value))} />
          {valor != null && <button type="button" className="boton boton-texto" onClick={() => onCambio(null)}>Quitar</button>}
        </div>
      )}
    </Campo>
  )
}

export function Duracion({ salida, llegada }: { salida: number | null; llegada: number | null }) {
  return (
    <AvisoInfo texto={salida != null && llegada != null && llegada >= salida
      ? `Duración: ${duracion(llegada - salida)}`
      : 'La duración se calcula con la salida y la llegada.'} />
  )
}

// ---------- Formulario en panel lateral ----------

/**
 * Panel a la derecha con el formulario. [onGuardar] devuelve null si se guardó (y se cierra)
 * o el mensaje de error, que se muestra arriba del botón.
 */
export function Cajon({ titulo, textoBoton, iconoBoton = 'check', habilitado, aviso, onGuardar, onCerrar, children }: {
  titulo: string; textoBoton: string; iconoBoton?: string; habilitado: boolean; aviso?: string | null
  onGuardar: () => string | null; onCerrar: () => void; children: ReactNode
}) {
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => { if (e.key === 'Escape') onCerrar() }
    window.addEventListener('keydown', tecla)
    return () => window.removeEventListener('keydown', tecla)
  }, [onCerrar])

  const guardar = () => {
    const e = onGuardar()
    setError(e)
    if (e == null) onCerrar()
  }

  return (
    <>
      <div className="velo" onClick={onCerrar} />
      <form className="cajon" role="dialog" aria-label={titulo} onSubmit={(e) => { e.preventDefault(); if (habilitado) guardar() }} onChange={() => setError(null)}>
        <header>
          <h2>{titulo}</h2>
          <button type="button" className="boton-icono" onClick={onCerrar} aria-label="Cerrar"><Icono nombre="close" /></button>
        </header>
        <div className="cuerpo">{children}</div>
        <footer>
          {error && <MensajeError texto={error} />}
          {aviso && <AvisoInfo texto={aviso} />}
          <button type="submit" className="boton boton-grande" disabled={!habilitado}>
            <Icono nombre={iconoBoton} relleno />{textoBoton}
          </button>
        </footer>
      </form>
    </>
  )
}
