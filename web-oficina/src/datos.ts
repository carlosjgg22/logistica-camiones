import { useEffect, useMemo, useState } from 'react'
import { collection, onSnapshot } from 'firebase/firestore'
import { db } from './firebase'
import {
  COLECCIONES, type Coleccion, type Datos, type EstadoViaje, type OperadorEstado, type TanqueDetalle,
  type UnidadDetalle, type VehiculoDetalle, type ViajeDetalle,
} from './tipos'

const VACIO: Datos = {
  tiposVehiculo: [], tiposCombustible: [], operadores: [], vehiculos: [], tanques: [],
  unidades: [], rutas: [], viajes: [], programaciones: [],
}

/**
 * Escucha en vivo las 9 colecciones. [ultimaSync] es la hora de la última respuesta del servidor
 * (no de la caché local), para el aviso "Datos actualizados a las HH:mm".
 */
export function useDatos() {
  const [datos, setDatos] = useState<Datos>(VACIO)
  const [ultimaSync, setUltimaSync] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const cancelar = (Object.keys(COLECCIONES) as Coleccion[]).map((clave) =>
      onSnapshot(
        collection(db, COLECCIONES[clave]),
        { includeMetadataChanges: true },
        (snap) => {
          // servidorTs es de la sincronización: no lo usan las pantallas
          const items = snap.docs.map((d) => {
            const { servidorTs: _ts, ...resto } = d.data()
            return { ...resto, id: d.id }
          })
          setDatos((prev) => ({ ...prev, [clave]: items }))
          if (!snap.metadata.fromCache) setUltimaSync(Date.now())
        },
        (e) => setError(e.code === 'permission-denied' ? 'Sin permiso para leer los datos.' : 'No se pudieron leer los datos.'),
      ),
    )
    return () => cancelar.forEach((c) => c())
  }, [])

  const vistas = useMemo(() => armarVistas(datos), [datos])
  return { datos, ...vistas, ultimaSync, error }
}

const enRuta = (v: { fechaSalida: number | null; fechaLlegada: number | null }) =>
  v.fechaSalida != null && v.fechaLlegada == null

const porTexto = <T,>(f: (x: T) => string) => (a: T, b: T) => f(a).localeCompare(f(b), 'es')

/** Lo que en la app hacen las consultas con JOIN de FlotaDao.kt. */
export function armarVistas(d: Datos) {
  const porId = <T extends { id: string }>(lista: T[]) => new Map(lista.map((x) => [x.id, x]))
  const rutas = porId(d.rutas)
  const unidades = porId(d.unidades)
  const vehiculos = porId(d.vehiculos)
  const tanques = porId(d.tanques)
  const operadores = porId(d.operadores)
  const combustibles = porId(d.tiposCombustible)
  const tiposVehiculo = porId(d.tiposVehiculo)
  const progDeViaje = new Map(d.programaciones.map((p) => [p.idViaje, p]))

  const viajes: ViajeDetalle[] = d.viajes.flatMap((v) => {
    const r = rutas.get(v.idRuta)
    if (!r) return [] // JOIN ruta: sin ruta no se muestra
    const u = v.idUnidad ? unidades.get(v.idUnidad) : undefined
    const ve = u ? vehiculos.get(u.idVehiculo) : undefined
    const t = u ? tanques.get(u.idTanque) : undefined
    const o = v.idOperador ? operadores.get(v.idOperador) : undefined
    const p = progDeViaje.get(v.id)
    const estado: EstadoViaje =
      v.fechaLlegada != null ? 'COMPLETADO'
        : v.fechaSalida != null ? 'EN_RUTA'
          : p ? 'PROGRAMADO' : 'SIN_PROGRAMAR'
    return [{
      ...v,
      numeroDespacho: r.numeroDespacho, recorrido: r.recorrido, numeroFactura: r.numeroFactura,
      vehiculo: ve?.matricula ?? null, tanque: t?.matricula ?? null,
      operador: o ? `${o.nombre} ${o.apellido}` : null,
      volumenLitros: t?.volumenLitros ?? null, compartimientos: t?.compartimientos ?? null,
      idProgramacion: p?.id ?? null, fechaProgramada: p?.fechaProgramada ?? null,
      carga: p ? combustibles.get(p.idTipoCombustible)?.nombre ?? null : null,
      estado,
      duracionMs: v.fechaSalida != null && v.fechaLlegada != null ? v.fechaLlegada - v.fechaSalida : null,
    }]
  })
  viajes.sort((a, b) =>
    (b.fechaSalida ?? b.fechaProgramada ?? b.actualizado) - (a.fechaSalida ?? a.fechaProgramada ?? a.actualizado))

  const enCurso = viajes
    .filter((v) => v.fechaLlegada == null && (v.idProgramacion != null || v.fechaSalida != null))
    .sort((a, b) => (a.fechaSalida ?? a.fechaProgramada ?? 0) - (b.fechaSalida ?? b.fechaProgramada ?? 0))

  const unidadesActivas = d.unidades.filter((u) => u.activa)
  const unidadesDetalle: UnidadDetalle[] = unidadesActivas.flatMap((u) => {
    const ve = vehiculos.get(u.idVehiculo)
    const t = tanques.get(u.idTanque)
    const o = operadores.get(u.idOperador)
    const c = t ? combustibles.get(t.idTipoCombustible) : undefined
    if (!ve || !t || !o || !c) return []
    return [{
      id: u.id, idOperador: u.idOperador,
      vehiculo: ve.matricula, descripcionVehiculo: ve.descripcion,
      tipoVehiculo: tiposVehiculo.get(ve.idTipoVehiculo)?.nombre ?? '',
      tanque: t.matricula, volumenLitros: t.volumenLitros, compartimientos: t.compartimientos,
      idTipoCombustible: t.idTipoCombustible, combustible: c.nombre,
      operador: `${o.nombre} ${o.apellido}`, nombreOperador: o.nombre,
      enUso: d.viajes.some((v) => v.idUnidad === u.id && enRuta(v)),
    }]
  }).sort(porTexto((u) => u.vehiculo))

  const operadoresEstado: OperadorEstado[] = d.operadores.filter((o) => o.activo).map((o) => {
    const unidad = unidadesActivas.find((u) => u.idOperador === o.id)
    // En viaje = salió y le falta la entrega del producto
    const viaje = d.viajes
      .filter((v) => v.idOperador === o.id && v.fechaSalida != null && v.fechaEntrega == null)
      .sort((a, b) => (b.fechaSalida ?? 0) - (a.fechaSalida ?? 0))[0]
    return {
      ...o,
      nombreCompleto: `${o.nombre} ${o.apellido}`,
      enUnidad: unidad != null,
      vehiculo: unidad ? vehiculos.get(unidad.idVehiculo)?.matricula ?? null : null,
      fechaSalida: viaje?.fechaSalida ?? null,
      destino: viaje?.destino ?? null,
      enViaje: viaje != null,
    }
  }).sort(porTexto((o) => o.nombreCompleto))

  const vehiculosDetalle: VehiculoDetalle[] = d.vehiculos.map((v) => ({
    ...v,
    tipo: tiposVehiculo.get(v.idTipoVehiculo)?.nombre ?? '',
    enUnidad: unidadesActivas.some((u) => u.idVehiculo === v.id),
  })).sort(porTexto((v) => v.matricula))

  const tanquesDetalle: TanqueDetalle[] = d.tanques.map((t) => ({
    ...t,
    combustible: combustibles.get(t.idTipoCombustible)?.nombre ?? '',
    enUnidad: unidadesActivas.some((u) => u.idTanque === t.id),
  })).sort(porTexto((t) => t.matricula))

  const rutasOrdenadas = [...d.rutas].sort((a, b) => b.numeroDespacho.localeCompare(a.numeroDespacho, 'es'))
  const combustiblesOrdenados = [...d.tiposCombustible].sort(porTexto((c) => c.nombre))
  const tiposVehiculoOrdenados = [...d.tiposVehiculo].sort(porTexto((t) => t.nombre))

  return {
    viajes, enCurso,
    unidades: unidadesDetalle,
    operadores: operadoresEstado,
    vehiculos: vehiculosDetalle,
    tanques: tanquesDetalle,
    rutas: rutasOrdenadas,
    combustibles: combustiblesOrdenados,
    tiposVehiculo: tiposVehiculoOrdenados,
  }
}

export type Vistas = ReturnType<typeof armarVistas>

/** Todo lo que ven las pantallas del panel: datos crudos, vistas armadas y estado de la conexión. */
export type DatosPanel = ReturnType<typeof useDatos>
