package com.flota.despacho.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

const val CONSULTA_VIAJE = """
    SELECT vj.id, vj.idRuta, vj.idUnidad, vj.idOperador, u.idVehiculo, u.idTanque,
           r.numeroDespacho, r.recorrido, r.numeroFactura, vj.destino,
           v.matricula AS vehiculo, t.matricula AS tanque, t.volumenLitros, t.compartimientos,
           o.nombre || ' ' || o.apellido AS operador,
           vj.fechaSalida, vj.fechaLlegada, vj.fechaEntrega,
           p.id AS idProgramacion, p.fechaProgramada, tc.nombre AS carga
    FROM viaje vj
    JOIN ruta r ON r.id = vj.idRuta
    LEFT JOIN unidad u ON u.id = vj.idUnidad
    LEFT JOIN vehiculo v ON v.id = u.idVehiculo
    LEFT JOIN tanque t ON t.id = u.idTanque
    LEFT JOIN operador o ON o.id = vj.idOperador
    LEFT JOIN programacion p ON p.idViaje = vj.id
    LEFT JOIN tipo_combustible tc ON tc.id = p.idTipoCombustible
"""

/** Un viaje está en ruta entre la salida y la llegada. */
const val EN_RUTA = "vj.fechaSalida IS NOT NULL AND vj.fechaLlegada IS NULL"

@Dao
interface FlotaDao {

    // ---------- Guardar (upsert: inserta o actualiza sin borrar) ----------
    @Upsert suspend fun guardarTiposVehiculo(items: List<TipoVehiculo>)
    @Upsert suspend fun guardarTiposCombustible(items: List<TipoCombustible>)
    @Upsert suspend fun guardarOperadores(items: List<Operador>)
    @Upsert suspend fun guardarVehiculos(items: List<Vehiculo>)
    @Upsert suspend fun guardarTanques(items: List<Tanque>)
    @Upsert suspend fun guardarUnidades(items: List<Unidad>)
    @Upsert suspend fun guardarRutas(items: List<Ruta>)
    @Upsert suspend fun guardarViajes(items: List<Viaje>)
    @Upsert suspend fun guardarProgramaciones(items: List<Programacion>)
    @Upsert suspend fun guardarInspecciones(items: List<Inspeccion>)
    @Upsert suspend fun guardarAverias(items: List<Averia>)

    @Query("SELECT COUNT(*) FROM operador")
    suspend fun contarOperadores(): Int

    // ---------- Búsquedas para validar ----------
    @Query("SELECT * FROM operador WHERE cedula = :cedula AND activo = 1 LIMIT 1")
    suspend fun operadorPorCedula(cedula: String): Operador?

    @Query("SELECT * FROM operador WHERE id = :id")
    suspend fun operador(id: String): Operador?

    @Query("SELECT * FROM vehiculo WHERE matricula = :matricula LIMIT 1")
    suspend fun vehiculoPorMatricula(matricula: String): Vehiculo?

    @Query("SELECT * FROM tanque WHERE matricula = :matricula LIMIT 1")
    suspend fun tanquePorMatricula(matricula: String): Tanque?

    @Query("SELECT * FROM unidad WHERE id = :id")
    suspend fun unidad(id: String): Unidad?

    @Query("SELECT * FROM viaje WHERE id = :id")
    suspend fun viaje(id: String): Viaje?

    @Query("SELECT * FROM programacion WHERE idViaje = :idViaje LIMIT 1")
    suspend fun programacionDeViaje(idViaje: String): Programacion?

    @Query(
        """
        SELECT EXISTS(SELECT 1 FROM unidad WHERE activa = 1
          AND (idVehiculo = :idVehiculo OR idTanque = :idTanque OR idOperador = :idOperador))
        """
    )
    suspend fun componenteEnUnidad(idVehiculo: String, idTanque: String, idOperador: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM viaje vj WHERE vj.idUnidad = :idUnidad AND vj.id != :excepto AND $EN_RUTA)")
    suspend fun unidadEnRuta(idUnidad: String, excepto: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM viaje vj WHERE vj.idOperador = :idOperador AND vj.id != :excepto AND $EN_RUTA)")
    suspend fun operadorEnRuta(idOperador: String, excepto: String): Boolean

    // ---------- Operador (chofer) ----------

    /** Salió y todavía le falta registrar la llegada o la entrega del producto. */
    @Query(
        CONSULTA_VIAJE +
            " WHERE vj.idOperador = :idOperador AND vj.fechaSalida IS NOT NULL" +
            " AND (vj.fechaLlegada IS NULL OR vj.fechaEntrega IS NULL)" +
            " ORDER BY vj.fechaSalida DESC LIMIT 1"
    )
    fun viajeAbierto(idOperador: String): Flow<ViajeDetalle?>

    @Query(
        CONSULTA_VIAJE +
            " WHERE vj.idOperador = :idOperador AND p.id IS NOT NULL AND vj.fechaSalida IS NULL" +
            " ORDER BY p.fechaProgramada LIMIT 1"
    )
    fun proximoViaje(idOperador: String): Flow<ViajeDetalle?>

    /** El último viaje que entregó, para mostrar "Viaje terminado". */
    @Query(
        CONSULTA_VIAJE +
            " WHERE vj.idOperador = :idOperador AND vj.fechaEntrega IS NOT NULL" +
            " ORDER BY vj.fechaEntrega DESC LIMIT 1"
    )
    fun ultimoTerminado(idOperador: String): Flow<ViajeDetalle?>

    // ---------- Listas de la oficina ----------
    @Query(CONSULTA_VIAJE + " ORDER BY COALESCE(vj.fechaSalida, p.fechaProgramada, vj.actualizado) DESC")
    fun viajes(): Flow<List<ViajeDetalle>>

    /** Programados que no salieron todavía, y los que están en ruta. */
    @Query(
        CONSULTA_VIAJE +
            " WHERE vj.fechaLlegada IS NULL AND (p.id IS NOT NULL OR vj.fechaSalida IS NOT NULL)" +
            " ORDER BY COALESCE(vj.fechaSalida, p.fechaProgramada)"
    )
    fun viajesEnCurso(): Flow<List<ViajeDetalle>>

    @Query(
        """
        SELECT o.id, o.nombre, o.apellido, o.cedula, o.telefono,
               EXISTS(SELECT 1 FROM unidad u WHERE u.idOperador = o.id AND u.activa = 1) AS enUnidad,
               (SELECT v.matricula FROM unidad u JOIN vehiculo v ON v.id = u.idVehiculo
                 WHERE u.idOperador = o.id AND u.activa = 1 LIMIT 1) AS vehiculo,
               (SELECT vj.fechaSalida FROM viaje vj WHERE vj.idOperador = o.id AND $EN_RUTA
                 ORDER BY vj.fechaSalida DESC LIMIT 1) AS fechaSalida,
               (SELECT vj.destino FROM viaje vj WHERE vj.idOperador = o.id AND $EN_RUTA
                 ORDER BY vj.fechaSalida DESC LIMIT 1) AS destino
        FROM operador o
        WHERE o.activo = 1
        ORDER BY o.nombre, o.apellido
        """
    )
    fun operadores(): Flow<List<OperadorEstado>>

    @Query(
        """
        SELECT u.id, u.idOperador, v.matricula AS vehiculo, v.descripcion AS descripcionVehiculo,
               t.matricula AS tanque, t.volumenLitros, t.compartimientos, t.idTipoCombustible,
               tc.nombre AS combustible, o.nombre || ' ' || o.apellido AS operador, o.nombre AS nombreOperador,
               EXISTS(SELECT 1 FROM viaje vj WHERE vj.idUnidad = u.id AND $EN_RUTA) AS enUso
        FROM unidad u
        JOIN vehiculo v ON v.id = u.idVehiculo
        JOIN tanque t ON t.id = u.idTanque
        JOIN tipo_combustible tc ON tc.id = t.idTipoCombustible
        JOIN operador o ON o.id = u.idOperador
        WHERE u.activa = 1
        ORDER BY v.matricula
        """
    )
    fun unidades(): Flow<List<UnidadDetalle>>

    @Query(
        """
        SELECT v.id, v.matricula, v.descripcion, v.idTipoVehiculo, tv.nombre AS tipo,
               EXISTS(SELECT 1 FROM unidad u WHERE u.idVehiculo = v.id AND u.activa = 1) AS enUnidad
        FROM vehiculo v JOIN tipo_vehiculo tv ON tv.id = v.idTipoVehiculo
        ORDER BY v.matricula
        """
    )
    fun vehiculos(): Flow<List<VehiculoDetalle>>

    @Query(
        """
        SELECT t.id, t.matricula, t.volumenLitros, t.compartimientos, t.idTipoCombustible,
               tc.nombre AS combustible,
               EXISTS(SELECT 1 FROM unidad u WHERE u.idTanque = t.id AND u.activa = 1) AS enUnidad
        FROM tanque t JOIN tipo_combustible tc ON tc.id = t.idTipoCombustible
        ORDER BY t.matricula
        """
    )
    fun tanques(): Flow<List<TanqueDetalle>>

    @Query("SELECT * FROM ruta ORDER BY numeroDespacho DESC")
    fun rutas(): Flow<List<Ruta>>

    @Query("SELECT * FROM tipo_vehiculo ORDER BY nombre")
    fun tiposVehiculo(): Flow<List<TipoVehiculo>>

    @Query("SELECT * FROM tipo_combustible ORDER BY nombre")
    fun combustibles(): Flow<List<TipoCombustible>>


    // ---------- Checklist y averías ----------

    /** Los checklists de este operador (para saber si ya hizo el de salida o el de llegada del viaje). */
    @Query("SELECT * FROM inspeccion WHERE idOperador = :idOperador")
    fun inspeccionesDe(idOperador: String): Flow<List<Inspeccion>>

    @Query("SELECT * FROM averia WHERE resuelta = 0 AND gravedad = 'GRAVE' ORDER BY fecha DESC")
    fun averiasGravesAbiertas(): Flow<List<Averia>>

    /** Una avería grave sin resolver en ese chuto o esa cisterna: bloquea la salida. */
    @Query(
        """
        SELECT * FROM averia WHERE resuelta = 0 AND gravedad = 'GRAVE'
          AND (idVehiculo = :idVehiculo OR idTanque = :idTanque)
        ORDER BY fecha DESC LIMIT 1
        """
    )
    suspend fun averiaGraveAbierta(idVehiculo: String, idTanque: String): Averia?

    @Query("SELECT EXISTS(SELECT 1 FROM inspeccion WHERE idViaje = :idViaje AND momento = :momento)")
    suspend fun hayInspeccion(idViaje: String, momento: String): Boolean
    // ---------- Sincronización: lo que falta subir ----------
    @Query("SELECT * FROM operador WHERE pendienteSync = 1") suspend fun operadoresPendientes(): List<Operador>
    @Query("SELECT * FROM vehiculo WHERE pendienteSync = 1") suspend fun vehiculosPendientes(): List<Vehiculo>
    @Query("SELECT * FROM tanque WHERE pendienteSync = 1") suspend fun tanquesPendientes(): List<Tanque>
    @Query("SELECT * FROM unidad WHERE pendienteSync = 1") suspend fun unidadesPendientes(): List<Unidad>
    @Query("SELECT * FROM ruta WHERE pendienteSync = 1") suspend fun rutasPendientes(): List<Ruta>
    @Query("SELECT * FROM viaje WHERE pendienteSync = 1") suspend fun viajesPendientes(): List<Viaje>
    @Query("SELECT * FROM programacion WHERE pendienteSync = 1") suspend fun programacionesPendientes(): List<Programacion>
    @Query("SELECT * FROM inspeccion WHERE pendienteSync = 1") suspend fun inspeccionesPendientes(): List<Inspeccion>
    @Query("SELECT * FROM averia WHERE pendienteSync = 1") suspend fun averiasPendientes(): List<Averia>

    // Solo marca como enviado si nadie lo modificó mientras se subía.
    @Query("UPDATE operador SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarOperadorEnviado(id: String, actualizado: Long)
    @Query("UPDATE vehiculo SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarVehiculoEnviado(id: String, actualizado: Long)
    @Query("UPDATE tanque SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarTanqueEnviado(id: String, actualizado: Long)
    @Query("UPDATE unidad SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarUnidadEnviada(id: String, actualizado: Long)
    @Query("UPDATE ruta SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarRutaEnviada(id: String, actualizado: Long)
    @Query("UPDATE viaje SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarViajeEnviado(id: String, actualizado: Long)
    @Query("UPDATE programacion SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarProgramacionEnviada(id: String, actualizado: Long)
    @Query("UPDATE inspeccion SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarInspeccionEnviada(id: String, actualizado: Long)
    @Query("UPDATE averia SET pendienteSync = 0 WHERE id = :id AND actualizado = :actualizado")
    suspend fun marcarAveriaEnviada(id: String, actualizado: Long)

    @Query(
        """
        SELECT (SELECT COUNT(*) FROM operador WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM vehiculo WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM tanque WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM unidad WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM ruta WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM viaje WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM programacion WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM inspeccion WHERE pendienteSync = 1)
             + (SELECT COUNT(*) FROM averia WHERE pendienteSync = 1)
        """
    )
    fun pendientes(): Flow<Int>
}
