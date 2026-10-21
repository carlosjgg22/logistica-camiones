package com.flota.despacho.data

import android.content.Context
import androidx.room.withTransaction
import com.flota.despacho.BuildConfig
import com.flota.despacho.data.db.*
import com.flota.despacho.sync.Colecciones
import com.flota.despacho.sync.FirebaseSync
import com.flota.despacho.sync.PaqueteSync
import com.flota.despacho.sync.SyncWorker
import com.google.gson.Gson
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Todo se guarda primero en el teléfono (Room) y se sincroniza con Firebase (Firestore)
 * en los dos sentidos cuando hay señal: sube lo registrado y baja lo de los demás.
 *
 * Las funciones que guardan devuelven null si salió bien, o el mensaje de error para mostrar.
 */
class FlotaRepository(private val db: AppDatabase, private val context: Context) {

    private val dao = db.dao()
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("sync", Context.MODE_PRIVATE)

    private val _ultimaSync = MutableStateFlow(prefs.getLong("ultimaSync", 0L).takeIf { it > 0 })
    val ultimaSync: StateFlow<Long?> = _ultimaSync.asStateFlow()
    val servidorConfigurado: Boolean get() = BuildConfig.FIREBASE

    // ---------- Lecturas ----------
    fun viajeAbierto(idOperador: String) = dao.viajeAbierto(idOperador)
    fun proximoViaje(idOperador: String) = dao.proximoViaje(idOperador)
    fun ultimoTerminado(idOperador: String) = dao.ultimoTerminado(idOperador)
    fun inspeccionesDe(idOperador: String) = dao.inspeccionesDe(idOperador)
    fun averiasGravesAbiertas() = dao.averiasGravesAbiertas()
    fun viajes() = dao.viajes()
    fun viajesEnCurso() = dao.viajesEnCurso()
    fun operadores() = dao.operadores()
    fun unidades() = dao.unidades()
    fun vehiculos() = dao.vehiculos()
    fun tanques() = dao.tanques()
    fun rutas() = dao.rutas()
    fun tiposVehiculo() = dao.tiposVehiculo()
    fun combustibles() = dao.combustibles()
    fun pendientes() = dao.pendientes()

    suspend fun operadorPorCedula(cedula: String) = dao.operadorPorCedula(cedula.trim())
    suspend fun operador(id: String) = dao.operador(id)

    // ---------- Administrador: correo y contraseña de Firebase Auth ----------

    /** Devuelve null si entró, o el mensaje de error para mostrar. */
    suspend fun entrarAdministrador(correo: String, clave: String): String? {
        if (!servidorConfigurado) return "Sin servidor configurado: el administrador necesita Firebase para entrar."
        return try {
            Firebase.auth.signInWithEmailAndPassword(correo.trim(), clave).await()
            pedirSincronizacion()
            null
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            "Correo o contraseña incorrectos."
        } catch (e: FirebaseAuthInvalidUserException) {
            "Correo o contraseña incorrectos."
        } catch (e: FirebaseNetworkException) {
            "Sin conexión. Revisa la señal e intenta de nuevo."
        } catch (e: FirebaseTooManyRequestsException) {
            "Demasiados intentos. Espera unos minutos e intenta de nuevo."
        } catch (e: FirebaseException) {
            "No se pudo iniciar sesión. Intenta de nuevo."
        }
    }

    /** true si hay un administrador con sesión de Firebase (no anónima). */
    val administradorConectado: Boolean
        get() = servidorConfigurado && Firebase.auth.currentUser?.isAnonymous == false

    /** Al salir vuelve la sesión anónima, la que usa la sincronización del operador. */
    fun cerrarSesionAdministrador() {
        if (servidorConfigurado) Firebase.auth.signOut()
    }

    /** Guarda y pide subirlo al servidor. */
    private suspend fun guardando(bloque: suspend () -> String?): String? {
        val error = db.withTransaction { bloque() }
        if (error == null) SyncWorker.programar(context)
        return error
    }

    // ---------- Catálogos (oficina) ----------

    suspend fun guardarOperador(o: Operador): String? = guardando {
        val otro = dao.operadorPorCedula(o.cedula)
        if (otro != null && otro.id != o.id) "Ya hay un operador con la cédula ${o.cedula} (${otro.nombre} ${otro.apellido})."
        else { dao.guardarOperadores(listOf(o.copy(pendienteSync = true, actualizado = ahora()))); null }
    }

    suspend fun guardarVehiculo(v: Vehiculo): String? = guardando {
        val otro = dao.vehiculoPorMatricula(v.matricula)
        if (otro != null && otro.id != v.id) "Ya hay un vehículo con la matrícula ${v.matricula}."
        else { dao.guardarVehiculos(listOf(v.copy(pendienteSync = true, actualizado = ahora()))); null }
    }

    suspend fun guardarTanque(t: Tanque): String? = guardando {
        val otro = dao.tanquePorMatricula(t.matricula)
        if (otro != null && otro.id != t.id) "Ya hay un tanque con la matrícula ${t.matricula}."
        else { dao.guardarTanques(listOf(t.copy(pendienteSync = true, actualizado = ahora()))); null }
    }

    suspend fun guardarRuta(r: Ruta): String? = guardando {
        dao.guardarRutas(listOf(r.copy(pendienteSync = true, actualizado = ahora())))
        null
    }

    // ---------- Unidades ----------

    suspend fun conformarUnidad(idVehiculo: String, idTanque: String, idOperador: String): String? = guardando {
        if (dao.componenteEnUnidad(idVehiculo, idTanque, idOperador)) {
            "El vehículo, el tanque o el operador ya están en otra unidad. Deshaz esa unidad primero."
        } else {
            dao.guardarUnidades(listOf(Unidad(idVehiculo = idVehiculo, idTanque = idTanque, idOperador = idOperador)))
            null
        }
    }

    suspend fun deshacerUnidad(idUnidad: String): String? = guardando {
        val u = dao.unidad(idUnidad)
        when {
            u == null -> "No se encontró la unidad."
            dao.unidadEnRuta(idUnidad, "") -> "La unidad está en ruta. Espera a que registre la llegada."
            else -> { dao.guardarUnidades(listOf(u.copy(activa = false, pendienteSync = true, actualizado = ahora()))); null }
        }
    }

    // ---------- Viajes ----------

    /** Alta o edición desde la oficina. Si trae unidad, el operador se toma de la unidad. */
    suspend fun guardarViaje(v: Viaje): String? {
        val tope = ahora() + MARGEN_MS
        val fechas = listOfNotNull(v.fechaSalida, v.fechaLlegada, v.fechaEntrega)
        when {
            fechas.any { it > tope } -> return "Las fechas no pueden ser posteriores a la hora actual."
            v.fechaSalida == null && (v.fechaLlegada != null || v.fechaEntrega != null) ->
                return "Para registrar la llegada o la entrega primero carga la fecha de salida."
            v.fechaLlegada != null && v.fechaLlegada < v.fechaSalida!! -> return "La llegada no puede ser anterior a la salida."
            v.fechaEntrega != null && v.fechaEntrega < v.fechaSalida!! -> return "La entrega no puede ser anterior a la salida."
        }
        return guardando {
            val unidad = v.idUnidad?.let { dao.unidad(it) }
            val idOperador = unidad?.idOperador ?: v.idOperador.takeIf { v.idUnidad != null }
            val enRuta = v.fechaSalida != null && v.fechaLlegada == null
            when {
                v.idUnidad != null && unidad == null -> "No se encontró la unidad."
                enRuta && dao.unidadEnRuta(v.idUnidad!!, v.id) -> "Esa unidad ya está en ruta en otro viaje."
                enRuta && idOperador != null && dao.operadorEnRuta(idOperador, v.id) -> "Ese operador ya está en ruta en otro viaje."
                else -> {
                    dao.guardarViajes(listOf(v.copy(idOperador = idOperador, pendienteSync = true, actualizado = ahora())))
                    null
                }
            }
        }
    }

    /** Programar = asignar unidad y tipo de carga a un viaje que todavía no salió. */
    suspend fun programarViaje(idViaje: String, idUnidad: String, idTipoCombustible: String, fecha: Long): String? = guardando {
        val viaje = dao.viaje(idViaje)
        val unidad = dao.unidad(idUnidad)
        when {
            viaje == null -> "No se encontró el viaje."
            unidad == null || !unidad.activa -> "Esa unidad ya no existe. Elige otra."
            viaje.fechaSalida != null -> "Ese viaje ya salió."
            dao.programacionDeViaje(idViaje) != null -> "Ese viaje ya está programado."
            else -> {
                val t = ahora()
                dao.guardarViajes(listOf(viaje.copy(idUnidad = idUnidad, idOperador = unidad.idOperador, pendienteSync = true, actualizado = t)))
                dao.guardarProgramaciones(listOf(Programacion(idViaje = idViaje, idTipoCombustible = idTipoCombustible, fechaProgramada = fecha, actualizado = t)))
                null
            }
        }
    }

    // ---------- Operador (chofer): salida, llegada y entrega ----------

    suspend fun registrarSalida(idViaje: String, fecha: Long): String? {
        if (fecha > ahora() + MARGEN_MS) return "La hora de salida no puede ser posterior a la hora actual."
        return guardando {
            val v = dao.viaje(idViaje)
            when {
                v == null -> "No se encontró el viaje."
                v.fechaSalida != null -> null // ya registrada, no hacemos nada
                v.idUnidad == null -> "El viaje no tiene unidad asignada. Llama a la oficina."
                v.idOperador != null && dao.operadorEnRuta(v.idOperador, v.id) -> "Ya tienes un viaje en curso."
                dao.unidadEnRuta(v.idUnidad, v.id) -> "Esa unidad ya está en ruta en otro viaje. Llama a la oficina."
                !dao.hayInspeccion(v.id, Momento.SALIDA.name) -> "Primero haz el checklist de salida."
                else -> averiaQueBloquea(v.idUnidad)?.let { a ->
                    "No puedes salir: hay una avería grave sin resolver (${a.codigo} ${a.punto}). Llama a la oficina."
                } ?: run {
                    dao.guardarViajes(listOf(v.copy(fechaSalida = fecha, pendienteSync = true, actualizado = ahora())))
                    null
                }
            }
        }
    }

    /** La avería grave sin resolver del chuto o la cisterna de esa unidad, si hay. */
    private suspend fun averiaQueBloquea(idUnidad: String): Averia? {
        val u = dao.unidad(idUnidad) ?: return null
        return dao.averiaGraveAbierta(u.idVehiculo, u.idTanque)
    }

    /**
     * Guarda el checklist de salida o de llegada y crea una avería por cada punto en M.
     * Las averías del chuto quedan en el vehículo y las de la cisterna en el tanque.
     */
    suspend fun guardarChecklist(
        idViaje: String,
        momento: Momento,
        kilometraje: Int,
        resultados: List<ResultadoPunto>,
        observaciones: String
    ): String? {
        val faltan = PUNTOS_CHECKLIST.filter { p -> resultados.none { it.codigo == p.codigo } }
        val sinDetalle = resultados.filter { it.resultado == Resultado.M.name && (it.gravedad == null || it.observacion.isBlank()) }
        when {
            faltan.isNotEmpty() -> return "Falta revisar: ${faltan.joinToString { it.codigo }}."
            sinDetalle.isNotEmpty() -> return "En los puntos en M elige leve o grave y escribe qué pasa: ${sinDetalle.joinToString { it.codigo }}."
        }
        return guardando {
            val v = dao.viaje(idViaje)
            val u = v?.idUnidad?.let { dao.unidad(it) }
            when {
                v == null || u == null -> "No se encontró el viaje o su unidad."
                dao.hayInspeccion(v.id, momento.name) -> null // ya estaba hecho
                else -> {
                    val t = ahora()
                    val inspeccion = Inspeccion(
                        idViaje = v.id, idUnidad = u.id, idOperador = u.idOperador, momento = momento.name,
                        fecha = t, kilometraje = kilometraje, resultados = gson.toJson(resultados),
                        observaciones = observaciones.trim(), actualizado = t
                    )
                    val averias = resultados.filter { it.resultado == Resultado.M.name }.map { r ->
                        val punto = PUNTOS_CHECKLIST.first { it.codigo == r.codigo }
                        Averia(
                            idInspeccion = inspeccion.id, idViaje = v.id, idUnidad = u.id, idOperador = u.idOperador,
                            idVehiculo = u.idVehiculo.takeIf { punto.seccion == Seccion.CHUTO },
                            idTanque = u.idTanque.takeIf { punto.seccion == Seccion.CISTERNA },
                            codigo = r.codigo, punto = punto.titulo, gravedad = r.gravedad!!,
                            observacion = r.observacion.trim(), momento = momento.name, fecha = t, actualizado = t
                        )
                    }
                    dao.guardarInspecciones(listOf(inspeccion))
                    dao.guardarAverias(averias)
                    null
                }
            }
        }
    }

    suspend fun registrarLlegada(idViaje: String, fecha: Long): String? {
        if (fecha > ahora() + MARGEN_MS) return "La hora de llegada no puede ser posterior a la hora actual."
        return guardando {
            val v = dao.viaje(idViaje)
            when {
                v?.fechaSalida == null -> "No se encontró el viaje."
                v.fechaLlegada != null -> null
                fecha < v.fechaSalida -> "La hora de llegada no puede ser anterior a la salida."
                // Una avería grave al llegar no bloquea la llegada: queda abierta para la oficina
                !dao.hayInspeccion(v.id, Momento.LLEGADA.name) -> "Primero haz el checklist de llegada."
                else -> { dao.guardarViajes(listOf(v.copy(fechaLlegada = fecha, pendienteSync = true, actualizado = ahora()))); null }
            }
        }
    }

    suspend fun registrarEntrega(idViaje: String, fecha: Long): String? {
        if (fecha > ahora() + MARGEN_MS) return "La hora de entrega no puede ser posterior a la hora actual."
        return guardando {
            val v = dao.viaje(idViaje)
            when {
                v?.fechaSalida == null -> "No se encontró el viaje."
                v.fechaEntrega != null -> null
                fecha < v.fechaSalida -> "La hora de entrega no puede ser anterior a la salida."
                else -> { dao.guardarViajes(listOf(v.copy(fechaEntrega = fecha, pendienteSync = true, actualizado = ahora()))); null }
            }
        }
    }

    // ---------- Sincronización ----------

    fun pedirSincronizacion() = SyncWorker.programar(context)

    /** La llama SyncWorker. Si falla lanza excepción y WorkManager reintenta. */
    suspend fun sincronizar() {
        if (!servidorConfigurado) return

        // 0) Proyecto de Firebase recién creado: lo llenamos con los datos de prueba
        if (FirebaseSync.estaVacio()) FirebaseSync.subirTodo(datosDemo())

        // 1) Subir lo registrado en este teléfono. Primero lo que otros registros referencian.
        dao.operadoresPendientes().forEach {
            FirebaseSync.enviar(Colecciones.OPERADORES, it.id, it); dao.marcarOperadorEnviado(it.id, it.actualizado)
        }
        dao.vehiculosPendientes().forEach {
            FirebaseSync.enviar(Colecciones.VEHICULOS, it.id, it); dao.marcarVehiculoEnviado(it.id, it.actualizado)
        }
        dao.tanquesPendientes().forEach {
            FirebaseSync.enviar(Colecciones.TANQUES, it.id, it); dao.marcarTanqueEnviado(it.id, it.actualizado)
        }
        dao.unidadesPendientes().forEach {
            FirebaseSync.enviar(Colecciones.UNIDADES, it.id, it); dao.marcarUnidadEnviada(it.id, it.actualizado)
        }
        dao.rutasPendientes().forEach {
            FirebaseSync.enviar(Colecciones.RUTAS, it.id, it); dao.marcarRutaEnviada(it.id, it.actualizado)
        }
        dao.viajesPendientes().forEach {
            FirebaseSync.enviar(Colecciones.VIAJES, it.id, it); dao.marcarViajeEnviado(it.id, it.actualizado)
        }
        dao.programacionesPendientes().forEach {
            FirebaseSync.enviar(Colecciones.PROGRAMACIONES, it.id, it); dao.marcarProgramacionEnviada(it.id, it.actualizado)
        }
        dao.inspeccionesPendientes().forEach {
            FirebaseSync.enviar(Colecciones.INSPECCIONES, it.id, it); dao.marcarInspeccionEnviada(it.id, it.actualizado)
        }
        dao.averiasPendientes().forEach {
            FirebaseSync.enviar(Colecciones.AVERIAS, it.id, it); dao.marcarAveriaEnviada(it.id, it.actualizado)
        }

        // 2) Bajar lo que cambió en Firestore (otros teléfonos: operadores, oficina)
        val desde = prefs.getLong(CLAVE_DESDE, 0L)
        val p = FirebaseSync.descargar(desde)
        db.withTransaction {
            dao.guardarTiposVehiculo(p.tiposVehiculo)
            dao.guardarTiposCombustible(p.tiposCombustible)
            // No pisar lo que este teléfono todavía no pudo subir
            val operadores = dao.operadoresPendientes().map { it.id }.toSet()
            dao.guardarOperadores(p.operadores.filter { it.id !in operadores }.map { it.copy(pendienteSync = false) })
            val vehiculos = dao.vehiculosPendientes().map { it.id }.toSet()
            dao.guardarVehiculos(p.vehiculos.filter { it.id !in vehiculos }.map { it.copy(pendienteSync = false) })
            val tanques = dao.tanquesPendientes().map { it.id }.toSet()
            dao.guardarTanques(p.tanques.filter { it.id !in tanques }.map { it.copy(pendienteSync = false) })
            val unidades = dao.unidadesPendientes().map { it.id }.toSet()
            dao.guardarUnidades(p.unidades.filter { it.id !in unidades }.map { it.copy(pendienteSync = false) })
            val rutas = dao.rutasPendientes().map { it.id }.toSet()
            dao.guardarRutas(p.rutas.filter { it.id !in rutas }.map { it.copy(pendienteSync = false) })
            val viajes = dao.viajesPendientes().map { it.id }.toSet()
            dao.guardarViajes(p.viajes.filter { it.id !in viajes }.map { it.copy(pendienteSync = false) })
            val programaciones = dao.programacionesPendientes().map { it.id }.toSet()
            dao.guardarProgramaciones(p.programaciones.filter { it.id !in programaciones }.map { it.copy(pendienteSync = false) })
            val inspecciones = dao.inspeccionesPendientes().map { it.id }.toSet()
            dao.guardarInspecciones(p.inspecciones.filter { it.id !in inspecciones }.map { it.copy(pendienteSync = false) })
            // Así llega al teléfono que la oficina resolvió una avería (y se destraba la salida)
            val averias = dao.averiasPendientes().map { it.id }.toSet()
            dao.guardarAverias(p.averias.filter { it.id !in averias }.map { it.copy(pendienteSync = false) })
        }
        val t = ahora()
        prefs.edit().putLong(CLAVE_DESDE, p.horaServidor).putLong("ultimaSync", t).apply()
        _ultimaSync.value = t
    }

    // ---------- Datos de ejemplo ----------

    /** Para probar. Con Firebase, borra esto cuando cargues tus datos reales en Firestore. */
    suspend fun cargarDatosDemoSiVacio() {
        if (dao.contarOperadores() > 0) return
        val d = datosDemo()
        db.withTransaction {
            dao.guardarTiposVehiculo(d.tiposVehiculo)
            dao.guardarTiposCombustible(d.tiposCombustible)
            dao.guardarOperadores(d.operadores)
            dao.guardarVehiculos(d.vehiculos)
            dao.guardarTanques(d.tanques)
            dao.guardarUnidades(d.unidades)
            dao.guardarRutas(d.rutas)
            dao.guardarViajes(d.viajes)
            dao.guardarProgramaciones(d.programaciones)
        }
    }

    /** Ids fijos: así todos los teléfonos y Firestore hablan de los mismos registros. */
    private fun datosDemo(): PaqueteSync {
        val t = ahora()
        val chuto = TipoVehiculo(id = "demo-tv-chuto", nombre = "Chuto")
        val camion = TipoVehiculo(id = "demo-tv-camion", nombre = "Camión")
        val gasoil = TipoCombustible(id = "demo-tc-gasoil", nombre = "Gasoil")
        val g91 = TipoCombustible(id = "demo-tc-g91", nombre = "Gasolina 91")
        val g95 = TipoCombustible(id = "demo-tc-g95", nombre = "Gasolina 95")

        val juan = Operador(id = "demo-op-juan", nombre = "Juan", apellido = "Pérez", cedula = "12345678", telefono = "04141234567", pendienteSync = false, actualizado = t)
        val maria = Operador(id = "demo-op-maria", nombre = "María", apellido = "Rodríguez", cedula = "87654321", telefono = "04247654321", pendienteSync = false, actualizado = t)
        val pedro = Operador(id = "demo-op-pedro", nombre = "Pedro", apellido = "Gómez", cedula = "11223344", telefono = "04125556677", pendienteSync = false, actualizado = t)

        val v1 = Vehiculo(id = "demo-ve-1", matricula = "A12BC3D", descripcion = "Mack Granite", idTipoVehiculo = chuto.id, pendienteSync = false, actualizado = t)
        val v2 = Vehiculo(id = "demo-ve-2", matricula = "A45EF6G", descripcion = "Freightliner M2", idTipoVehiculo = chuto.id, pendienteSync = false, actualizado = t)
        val v3 = Vehiculo(id = "demo-ve-3", matricula = "A78HJ9K", descripcion = "International 9200", idTipoVehiculo = chuto.id, pendienteSync = false, actualizado = t)
        val t1 = Tanque(id = "demo-ta-1", matricula = "X1234A", volumenLitros = 36000, compartimientos = 3, idTipoCombustible = gasoil.id, pendienteSync = false, actualizado = t)
        val t2 = Tanque(id = "demo-ta-2", matricula = "X5678B", volumenLitros = 38000, compartimientos = 4, idTipoCombustible = g91.id, pendienteSync = false, actualizado = t)
        val t3 = Tanque(id = "demo-ta-3", matricula = "X9012C", volumenLitros = 30000, compartimientos = 2, idTipoCombustible = g95.id, pendienteSync = false, actualizado = t)
        // Pedro, el tercer vehículo y el tercer tanque quedan libres para probar "Conformar unidad"
        val u1 = Unidad(id = "demo-un-1", idVehiculo = v1.id, idTanque = t1.id, idOperador = juan.id, pendienteSync = false, actualizado = t)
        val u2 = Unidad(id = "demo-un-2", idVehiculo = v2.id, idTanque = t2.id, idOperador = maria.id, pendienteSync = false, actualizado = t)

        val r1 = Ruta(id = "demo-ru-1", numeroDespacho = "D-0001", recorrido = "Planta principal, autopista, salida 4", numeroFactura = "F-10021", pendienteSync = false, actualizado = t)
        val r2 = Ruta(id = "demo-ru-2", numeroDespacho = "D-0002", recorrido = "Planta principal, carretera nacional", numeroFactura = "F-10022", pendienteSync = false, actualizado = t)
        val r3 = Ruta(id = "demo-ru-3", numeroDespacho = "D-0003", recorrido = "Planta principal, avenida Bolívar", numeroFactura = "F-10023", pendienteSync = false, actualizado = t)

        val vj1 = Viaje(id = "demo-vj-1", idRuta = r1.id, idUnidad = u1.id, idOperador = juan.id, destino = "Estación Centro", pendienteSync = false, actualizado = t)
        val vj2 = Viaje(id = "demo-vj-2", idRuta = r2.id, idUnidad = u2.id, idOperador = maria.id, destino = "Estación Norte", pendienteSync = false, actualizado = t)
        val vj3 = Viaje(id = "demo-vj-3", idRuta = r3.id, destino = "Estación Sur", pendienteSync = false, actualizado = t)

        return PaqueteSync(
            horaServidor = 0,
            tiposVehiculo = listOf(chuto, camion),
            tiposCombustible = listOf(gasoil, g91, g95),
            operadores = listOf(juan, maria, pedro),
            vehiculos = listOf(v1, v2, v3),
            tanques = listOf(t1, t2, t3),
            unidades = listOf(u1, u2),
            rutas = listOf(r1, r2, r3),
            viajes = listOf(vj1, vj2, vj3),
            programaciones = listOf(
                Programacion(id = "demo-pr-1", idViaje = vj1.id, idTipoCombustible = gasoil.id, fechaProgramada = t, pendienteSync = false, actualizado = t),
                Programacion(id = "demo-pr-2", idViaje = vj2.id, idTipoCombustible = g91.id, fechaProgramada = t, pendienteSync = false, actualizado = t)
            )
        )
    }

    private companion object {
        const val MARGEN_MS = 60_000L
        /** Cambia junto con las colecciones (_v2): así se descarga todo de nuevo. */
        const val CLAVE_DESDE = "desde_v2"
    }
}
