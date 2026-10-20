// Los mismos registros que app/src/main/java/com/flota/despacho/data/db/Entidades.kt.
// En Firestore cada documento tiene estos campos (sin pendienteSync) más servidorTs.

export interface TipoVehiculo { id: string; nombre: string }
export interface TipoCombustible { id: string; nombre: string }

export interface Operador {
  id: string; nombre: string; apellido: string; cedula: string; telefono: string
  activo: boolean; actualizado: number
}

export interface Vehiculo {
  id: string; matricula: string; descripcion: string; idTipoVehiculo: string; actualizado: number
}

export interface Tanque {
  id: string; matricula: string; volumenLitros: number; compartimientos: number
  idTipoCombustible: string; actualizado: number
}

/** Unidad = vehículo + tanque + operador. Al deshacerla queda inactiva. */
export interface Unidad {
  id: string; idVehiculo: string; idTanque: string; idOperador: string; activa: boolean; actualizado: number
}

export interface Ruta {
  id: string; numeroDespacho: string; recorrido: string; numeroFactura: string; actualizado: number
}

export interface Viaje {
  id: string; idRuta: string; idUnidad: string | null; idOperador: string | null; destino: string
  fechaSalida: number | null; fechaLlegada: number | null; fechaEntrega: number | null; actualizado: number
}

export interface Programacion {
  id: string; idViaje: string; idTipoCombustible: string; fechaProgramada: number; actualizado: number
}

/** Nombres de las colecciones (con el mismo sufijo que la app: FirebaseSync.Colecciones). */
export const COLECCIONES = {
  tiposVehiculo: 'tiposVehiculo_v2',
  tiposCombustible: 'tiposCombustible_v2',
  operadores: 'operadores_v2',
  vehiculos: 'vehiculos_v2',
  tanques: 'tanques_v2',
  unidades: 'unidades_v2',
  rutas: 'rutas_v2',
  viajes: 'viajes_v2',
  programaciones: 'programaciones_v2',
} as const

export type Coleccion = keyof typeof COLECCIONES

export interface Datos {
  tiposVehiculo: TipoVehiculo[]
  tiposCombustible: TipoCombustible[]
  operadores: Operador[]
  vehiculos: Vehiculo[]
  tanques: Tanque[]
  unidades: Unidad[]
  rutas: Ruta[]
  viajes: Viaje[]
  programaciones: Programacion[]
}

export type EstadoViaje = 'SIN_PROGRAMAR' | 'PROGRAMADO' | 'EN_RUTA' | 'COMPLETADO'

export const ETIQUETA_ESTADO: Record<EstadoViaje, string> = {
  SIN_PROGRAMAR: 'Sin programar',
  PROGRAMADO: 'Programado',
  EN_RUTA: 'En ruta',
  COMPLETADO: 'Completado',
}

export const ESTADOS: EstadoViaje[] = ['SIN_PROGRAMAR', 'PROGRAMADO', 'EN_RUTA', 'COMPLETADO']

// ---------- Vistas armadas en el cliente (lo que en Room es un JOIN) ----------

export interface ViajeDetalle extends Viaje {
  numeroDespacho: string; recorrido: string; numeroFactura: string
  vehiculo: string | null; tanque: string | null; operador: string | null
  volumenLitros: number | null; compartimientos: number | null
  idProgramacion: string | null; fechaProgramada: number | null; carga: string | null
  estado: EstadoViaje
  duracionMs: number | null
}

export interface UnidadDetalle {
  id: string; idOperador: string; vehiculo: string; descripcionVehiculo: string; tipoVehiculo: string
  tanque: string; volumenLitros: number; compartimientos: number; idTipoCombustible: string; combustible: string
  operador: string; nombreOperador: string; enUso: boolean
}

export interface OperadorEstado extends Operador {
  nombreCompleto: string; enUnidad: boolean; vehiculo: string | null
  fechaSalida: number | null; destino: string | null; enViaje: boolean
}

export interface VehiculoDetalle extends Vehiculo { tipo: string; enUnidad: boolean }
export interface TanqueDetalle extends Tanque { combustible: string; enUnidad: boolean }
