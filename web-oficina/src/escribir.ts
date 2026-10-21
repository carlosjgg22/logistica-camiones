import { doc, serverTimestamp, writeBatch } from 'firebase/firestore'
import { db } from './firebase'
import {
  COLECCIONES, type Coleccion, type Datos, type Operador, type Programacion, type Ruta, type Tanque,
  type Unidad, type Vehiculo, type Viaje,
} from './tipos'

// Mismas validaciones y mensajes que data/FlotaRepository.kt de la app.
// Cada función devuelve null si se guardó, o el mensaje de error para mostrar.

export const nuevoId = () => crypto.randomUUID()

/** No se aceptan horas más de un minuto en el futuro (igual que la app). */
const MARGEN_MS = 60_000

/**
 * La escritura se aplica al instante en la caché local (la pantalla se actualiza sola) y Firestore
 * la sube cuando hay conexión. Si el servidor la rechaza, se avisa por este evento.
 */
export const erroresEscritura = new EventTarget()

function escribir<R extends { id: string }>(cambios: { coleccion: Coleccion; registro: R }[]) {
  const lote = writeBatch(db)
  for (const { coleccion, registro } of cambios) {
    // servidorTs: la hora del servidor, que usan los teléfonos para bajar solo lo nuevo
    lote.set(doc(db, COLECCIONES[coleccion], registro.id), { ...registro, servidorTs: serverTimestamp() })
  }
  lote.commit().catch((e: { code?: string }) => {
    const texto = e.code === 'permission-denied'
      ? 'El servidor rechazó el cambio: tu usuario no tiene permiso.'
      : 'No se pudo guardar en el servidor. Revisa la conexión.'
    erroresEscritura.dispatchEvent(new CustomEvent('error', { detail: texto }))
  })
}

const enRuta = (v: Viaje) => v.fechaSalida != null && v.fechaLlegada == null

// ---------- Catálogos ----------

export function guardarOperador(d: Datos, o: Omit<Operador, 'activo' | 'actualizado'> & { activo?: boolean }): string | null {
  const otro = d.operadores.find((x) => x.cedula === o.cedula && x.activo)
  if (otro && otro.id !== o.id) return `Ya hay un operador con la cédula ${o.cedula} (${otro.nombre} ${otro.apellido}).`
  escribir([{ coleccion: 'operadores', registro: { activo: true, ...o, actualizado: Date.now() } }])
  return null
}

export function guardarVehiculo(d: Datos, v: Omit<Vehiculo, 'actualizado'>): string | null {
  const otro = d.vehiculos.find((x) => x.matricula === v.matricula)
  if (otro && otro.id !== v.id) return `Ya hay un vehículo con la matrícula ${v.matricula}.`
  escribir([{ coleccion: 'vehiculos', registro: { ...v, actualizado: Date.now() } }])
  return null
}

export function guardarTanque(d: Datos, t: Omit<Tanque, 'actualizado'>): string | null {
  const otro = d.tanques.find((x) => x.matricula === t.matricula)
  if (otro && otro.id !== t.id) return `Ya hay un tanque con la matrícula ${t.matricula}.`
  escribir([{ coleccion: 'tanques', registro: { ...t, actualizado: Date.now() } }])
  return null
}

export function guardarRuta(r: Omit<Ruta, 'actualizado'>): string | null {
  escribir([{ coleccion: 'rutas', registro: { ...r, actualizado: Date.now() } }])
  return null
}

// ---------- Unidades ----------

export function conformarUnidad(d: Datos, idVehiculo: string, idTanque: string, idOperador: string): string | null {
  const ocupado = d.unidades.some((u) =>
    u.activa && (u.idVehiculo === idVehiculo || u.idTanque === idTanque || u.idOperador === idOperador))
  if (ocupado) return 'El vehículo, el tanque o el operador ya están en otra unidad. Deshaz esa unidad primero.'
  const u: Unidad = { id: nuevoId(), idVehiculo, idTanque, idOperador, activa: true, actualizado: Date.now() }
  escribir([{ coleccion: 'unidades', registro: u }])
  return null
}

export function deshacerUnidad(d: Datos, idUnidad: string): string | null {
  const u = d.unidades.find((x) => x.id === idUnidad)
  if (!u) return 'No se encontró la unidad.'
  if (d.viajes.some((v) => v.idUnidad === idUnidad && enRuta(v))) return 'La unidad está en ruta. Espera a que registre la llegada.'
  escribir([{ coleccion: 'unidades', registro: { ...u, activa: false, actualizado: Date.now() } }])
  return null
}

// ---------- Viajes ----------

/** Alta o edición. Si trae unidad, el operador se toma de la unidad. */
export function guardarViaje(d: Datos, v: Omit<Viaje, 'actualizado' | 'idOperador'> & { idOperador?: string | null }): string | null {
  const tope = Date.now() + MARGEN_MS
  const fechas = [v.fechaSalida, v.fechaLlegada, v.fechaEntrega].filter((f): f is number => f != null)
  if (fechas.some((f) => f > tope)) return 'Las fechas no pueden ser posteriores a la hora actual.'
  if (v.fechaSalida == null && (v.fechaLlegada != null || v.fechaEntrega != null))
    return 'Para registrar la llegada o la entrega primero carga la fecha de salida.'
  if (v.fechaSalida != null && v.fechaLlegada != null && v.fechaLlegada < v.fechaSalida) return 'La llegada no puede ser anterior a la salida.'
  if (v.fechaSalida != null && v.fechaEntrega != null && v.fechaEntrega < v.fechaSalida) return 'La entrega no puede ser anterior a la salida.'

  const unidad = v.idUnidad ? d.unidades.find((u) => u.id === v.idUnidad) : undefined
  if (v.idUnidad && !unidad) return 'No se encontró la unidad.'
  const idOperador = unidad?.idOperador ?? (v.idUnidad ? v.idOperador ?? null : null)
  const sale = v.fechaSalida != null && v.fechaLlegada == null
  if (sale && d.viajes.some((x) => x.id !== v.id && x.idUnidad === v.idUnidad && v.idUnidad && enRuta(x)))
    return 'Esa unidad ya está en ruta en otro viaje.'
  if (sale && idOperador && d.viajes.some((x) => x.id !== v.id && x.idOperador === idOperador && enRuta(x)))
    return 'Ese operador ya está en ruta en otro viaje.'

  const registro: Viaje = { ...v, idOperador, actualizado: Date.now() }
  escribir([{ coleccion: 'viajes', registro }])
  return null
}

/** Programar = asignar unidad y tipo de carga a un viaje que todavía no salió. */
export function programarViaje(d: Datos, idViaje: string, idUnidad: string, idTipoCombustible: string, fecha: number): string | null {
  const viaje = d.viajes.find((v) => v.id === idViaje)
  const unidad = d.unidades.find((u) => u.id === idUnidad)
  if (!viaje) return 'No se encontró el viaje.'
  if (!unidad || !unidad.activa) return 'Esa unidad ya no existe. Elige otra.'
  if (viaje.fechaSalida != null) return 'Ese viaje ya salió.'
  if (d.programaciones.some((p) => p.idViaje === idViaje)) return 'Ese viaje ya está programado.'
  const t = Date.now()
  const programacion: Programacion = { id: nuevoId(), idViaje, idTipoCombustible, fechaProgramada: fecha, actualizado: t }
  const conUnidad: Viaje = { ...viaje, idUnidad, idOperador: unidad.idOperador, actualizado: t }
  escribir<Viaje | Programacion>([
    { coleccion: 'viajes', registro: conUnidad },
    { coleccion: 'programaciones', registro: programacion },
  ])
  return null
}

// ---------- Averías ----------

/** La oficina marca resuelta una avería: si era grave, el chuto o la cisterna vuelven a poder salir. */
export function resolverAveria(d: Datos, idAveria: string, quien: string | null): string | null {
  const a = d.averias.find((x) => x.id === idAveria)
  if (!a) return 'No se encontró la avería.'
  if (a.resuelta) return null
  const t = Date.now()
  escribir([{ coleccion: 'averias', registro: { ...a, resuelta: true, fechaResuelta: t, resueltaPor: quien, actualizado: t } }])
  return null
}
