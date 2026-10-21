package com.flota.despacho.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/** IDs UUID: se crean en el teléfono sin conexión y no chocan al sincronizar. */
fun nuevoId(): String = UUID.randomUUID().toString()

fun ahora(): Long = System.currentTimeMillis()

// ---------- Catálogos fijos (se cargan desde el servidor) ----------

@Entity(tableName = "tipo_vehiculo")
data class TipoVehiculo(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String
)

@Entity(tableName = "tipo_combustible")
data class TipoCombustible(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String
)

// ---------- Lo que se carga desde la oficina ----------
// pendienteSync = todavía no se subió al servidor. actualizado = para no marcar como enviado
// algo que se volvió a modificar mientras se subía.

@Entity(tableName = "operador", indices = [Index(value = ["cedula"], unique = true)])
data class Operador(
    @PrimaryKey val id: String = nuevoId(),
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val telefono: String,
    val activo: Boolean = true,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

@Entity(
    tableName = "vehiculo",
    foreignKeys = [
        ForeignKey(entity = TipoVehiculo::class, parentColumns = ["id"], childColumns = ["idTipoVehiculo"])
    ],
    indices = [Index(value = ["matricula"], unique = true), Index("idTipoVehiculo")]
)
data class Vehiculo(
    @PrimaryKey val id: String = nuevoId(),
    val matricula: String,
    val descripcion: String,
    val idTipoVehiculo: String,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

@Entity(
    tableName = "tanque",
    foreignKeys = [
        ForeignKey(entity = TipoCombustible::class, parentColumns = ["id"], childColumns = ["idTipoCombustible"])
    ],
    indices = [Index(value = ["matricula"], unique = true), Index("idTipoCombustible")]
)
data class Tanque(
    @PrimaryKey val id: String = nuevoId(),
    val matricula: String,
    val volumenLitros: Int,
    val compartimientos: Int,
    val idTipoCombustible: String,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

/** Unidad = vehículo + tanque + operador. Al deshacerla queda inactiva para conservar el historial. */
@Entity(
    tableName = "unidad",
    foreignKeys = [
        ForeignKey(entity = Vehiculo::class, parentColumns = ["id"], childColumns = ["idVehiculo"]),
        ForeignKey(entity = Tanque::class, parentColumns = ["id"], childColumns = ["idTanque"]),
        ForeignKey(entity = Operador::class, parentColumns = ["id"], childColumns = ["idOperador"])
    ],
    indices = [Index("idVehiculo"), Index("idTanque"), Index("idOperador")]
)
data class Unidad(
    @PrimaryKey val id: String = nuevoId(),
    val idVehiculo: String,
    val idTanque: String,
    val idOperador: String,
    val activa: Boolean = true,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

@Entity(tableName = "ruta")
data class Ruta(
    @PrimaryKey val id: String = nuevoId(),
    val numeroDespacho: String,
    val recorrido: String,
    val numeroFactura: String,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

/**
 * El viaje se carga en la oficina. La unidad puede quedar vacía hasta programarlo.
 * El operador se copia de la unidad al asignarla, así el historial no cambia si la unidad se rearma.
 */
@Entity(
    tableName = "viaje",
    foreignKeys = [
        ForeignKey(entity = Ruta::class, parentColumns = ["id"], childColumns = ["idRuta"]),
        ForeignKey(entity = Unidad::class, parentColumns = ["id"], childColumns = ["idUnidad"]),
        ForeignKey(entity = Operador::class, parentColumns = ["id"], childColumns = ["idOperador"])
    ],
    indices = [Index("idRuta"), Index("idUnidad"), Index("idOperador")]
)
data class Viaje(
    @PrimaryKey val id: String = nuevoId(),
    val idRuta: String,
    val idUnidad: String? = null,
    val idOperador: String? = null,
    val destino: String,
    val fechaSalida: Long? = null,
    val fechaLlegada: Long? = null,
    val fechaEntrega: Long? = null,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

/** Programar = asignarle a un viaje la unidad y el tipo de carga. Un viaje se programa una sola vez. */
@Entity(
    tableName = "programacion",
    foreignKeys = [
        ForeignKey(entity = Viaje::class, parentColumns = ["id"], childColumns = ["idViaje"]),
        ForeignKey(entity = TipoCombustible::class, parentColumns = ["id"], childColumns = ["idTipoCombustible"])
    ],
    indices = [Index(value = ["idViaje"], unique = true), Index("idTipoCombustible")]
)
data class Programacion(
    @PrimaryKey val id: String = nuevoId(),
    val idViaje: String,
    val idTipoCombustible: String,
    val fechaProgramada: Long,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

/**
 * Checklist CHK-CTR-01 hecho por el operador al salir o al llegar.
 * [resultados] es JSON: [{"codigo":"1.1","resultado":"B","gravedad":null,"observacion":""}, …].
 * Sin claves foráneas: puede bajar del servidor antes que su viaje sin romper la sincronización.
 */
@Entity(tableName = "inspeccion", indices = [Index("idViaje"), Index("idOperador")])
data class Inspeccion(
    @PrimaryKey val id: String = nuevoId(),
    val idViaje: String,
    val idUnidad: String,
    val idOperador: String,
    /** SALIDA o LLEGADA */
    val momento: String,
    val fecha: Long,
    val kilometraje: Int,
    val resultados: String,
    val observaciones: String,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

/**
 * Un punto del checklist marcado M. Queda en el chuto ([idVehiculo]) o en la cisterna ([idTanque]),
 * así sigue valiendo aunque la unidad se deshaga. Una GRAVE sin resolver bloquea la salida.
 */
@Entity(tableName = "averia", indices = [Index("idVehiculo"), Index("idTanque"), Index("idViaje")])
data class Averia(
    @PrimaryKey val id: String = nuevoId(),
    val idInspeccion: String,
    val idViaje: String,
    val idUnidad: String,
    val idOperador: String,
    val idVehiculo: String?,
    val idTanque: String?,
    val codigo: String,
    val punto: String,
    /** LEVE o GRAVE */
    val gravedad: String,
    val observacion: String,
    val momento: String,
    val fecha: Long,
    val resuelta: Boolean = false,
    val fechaResuelta: Long? = null,
    val resueltaPor: String? = null,
    val pendienteSync: Boolean = true,
    val actualizado: Long = ahora()
)

enum class EstadoViaje(val etiqueta: String) {
    SIN_PROGRAMAR("Sin programar"),
    PROGRAMADO("Programado"),
    EN_RUTA("En ruta"),
    COMPLETADO("Completado")
}

// ---------- Vistas para pantallas (resultados de JOIN) ----------

data class ViajeDetalle(
    val id: String,
    val idRuta: String,
    val idUnidad: String?,
    val idOperador: String?,
    val idVehiculo: String?,
    val idTanque: String?,
    val numeroDespacho: String,
    val recorrido: String,
    val numeroFactura: String,
    val destino: String,
    val vehiculo: String?,
    val tanque: String?,
    val volumenLitros: Int?,
    val compartimientos: Int?,
    val operador: String?,
    val fechaSalida: Long?,
    val fechaLlegada: Long?,
    val fechaEntrega: Long?,
    val idProgramacion: String?,
    val fechaProgramada: Long?,
    val carga: String?
) {
    val estado: EstadoViaje
        get() = when {
            fechaLlegada != null -> EstadoViaje.COMPLETADO
            fechaSalida != null -> EstadoViaje.EN_RUTA
            idProgramacion != null -> EstadoViaje.PROGRAMADO
            else -> EstadoViaje.SIN_PROGRAMAR
        }

    /** Salida a llegada, cuando ya están las dos. */
    val duracionMs: Long?
        get() = if (fechaSalida != null && fechaLlegada != null) fechaLlegada - fechaSalida else null
}

data class UnidadDetalle(
    val id: String,
    val idOperador: String,
    val vehiculo: String,
    val descripcionVehiculo: String,
    val tanque: String,
    val volumenLitros: Int,
    val compartimientos: Int,
    val idTipoCombustible: String,
    val combustible: String,
    val operador: String,
    val nombreOperador: String,
    val enUso: Boolean
)

data class VehiculoDetalle(
    val id: String,
    val matricula: String,
    val descripcion: String,
    val idTipoVehiculo: String,
    val tipo: String,
    val enUnidad: Boolean
)

data class TanqueDetalle(
    val id: String,
    val matricula: String,
    val volumenLitros: Int,
    val compartimientos: Int,
    val idTipoCombustible: String,
    val combustible: String,
    val enUnidad: Boolean
)

data class OperadorEstado(
    val id: String,
    val nombre: String,
    val apellido: String,
    val cedula: String,
    val telefono: String,
    val enUnidad: Boolean,
    val vehiculo: String?,
    val fechaSalida: Long?,
    val destino: String?
)

val OperadorEstado.enViaje: Boolean get() = fechaSalida != null
val OperadorEstado.nombreCompleto: String get() = "$nombre $apellido"
