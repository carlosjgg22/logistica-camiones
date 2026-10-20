// Mismos formatos que ui/componentes/Componentes.kt de la app.

const dos = (n: number) => String(n).padStart(2, '0')

export const hora = (ms: number) => {
  const d = new Date(ms)
  return `${dos(d.getHours())}:${dos(d.getMinutes())}`
}

export const fechaHora = (ms: number) => {
  const d = new Date(ms)
  return `${dos(d.getDate())}/${dos(d.getMonth() + 1)}/${d.getFullYear()}, ${hora(ms)}`
}

/** "Martes 20 de octubre" */
export const fecha = (ms: number) => {
  const t = new Intl.DateTimeFormat('es', { weekday: 'long', day: 'numeric', month: 'long' }).format(new Date(ms))
  return t.charAt(0).toUpperCase() + t.slice(1)
}

/** "hoy", "mañana", "ayer" o la fecha en minúscula: para "Programado para hoy". */
export const diaRelativo = (ms: number, ahora = Date.now()) => {
  const dia = (x: number) => { const d = new Date(x); d.setHours(0, 0, 0, 0); return d.getTime() }
  const dif = Math.round((dia(ms) - dia(ahora)) / 86_400_000)
  if (dif === 0) return 'hoy'
  if (dif === 1) return 'mañana'
  if (dif === -1) return 'ayer'
  return 'el ' + fecha(ms).toLowerCase()
}

export const litros = (l: number) => new Intl.NumberFormat('es').format(l) + ' L'

export const duracion = (ms: number) => {
  const minutos = Math.max(0, Math.floor(ms / 60_000))
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  return h > 0 ? `${h} h ${m} min` : `${m} min`
}

export const cantidad = (n: number, uno: string, varios: string) => `${n} ${n === 1 ? uno : varios}`

// <input type="datetime-local"> y <input type="date"> trabajan con la hora local en texto
export const aInputFechaHora = (ms: number | null) => {
  if (ms == null) return ''
  const d = new Date(ms)
  return `${d.getFullYear()}-${dos(d.getMonth() + 1)}-${dos(d.getDate())}T${hora(ms)}`
}
export const deInputFechaHora = (v: string) => (v ? new Date(v).getTime() : null)

export const aInputFecha = (ms: number) => aInputFechaHora(ms).slice(0, 10)
/** Igual que la app: el día elegido a las 6:00 hora local. */
export const deInputFecha = (v: string) => new Date(`${v}T06:00`).getTime()
