package com.flota.despacho.sync

import com.flota.despacho.data.db.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/** Lo que cambió en Firestore desde la última descarga. */
data class PaqueteSync(
    val horaServidor: Long,
    val tiposVehiculo: List<TipoVehiculo>,
    val tiposCombustible: List<TipoCombustible>,
    val operadores: List<Operador>,
    val vehiculos: List<Vehiculo>,
    val tanques: List<Tanque>,
    val unidades: List<Unidad>,
    val rutas: List<Ruta>,
    val viajes: List<Viaje>,
    val programaciones: List<Programacion>
)

/**
 * Nombres de las colecciones en Firestore. Llevan versión porque los documentos viejos
 * (con otra estructura) no se pueden leer con las clases nuevas: así quedan ignorados.
 */
object Colecciones {
    private const val V = "_v2"
    const val TIPOS_VEHICULO = "tiposVehiculo$V"
    const val TIPOS_COMBUSTIBLE = "tiposCombustible$V"
    const val OPERADORES = "operadores$V"
    const val VEHICULOS = "vehiculos$V"
    const val TANQUES = "tanques$V"
    const val UNIDADES = "unidades$V"
    const val RUTAS = "rutas$V"
    const val VIAJES = "viajes$V"
    const val PROGRAMACIONES = "programaciones$V"
}

/**
 * Sincronización con Cloud Firestore. Una colección por tabla, el id del documento es el UUID
 * del registro: escribir dos veces el mismo registro no duplica nada.
 * Cada documento lleva `servidorTs` (hora del servidor) para bajar solo lo que cambió.
 */
object FirebaseSync {
    private const val TS = "servidorTs"
    private const val TIEMPO_MAX_MS = 30_000L
    /** Se vuelve a pedir un poco hacia atrás por si un envío se confirmó con hora anterior. */
    private const val SOLAPE_MS = 60_000L

    // LONG_OR_DOUBLE: que litros y fechas lleguen a Firestore como enteros, no como 36000.0
    private val gson: Gson = GsonBuilder()
        .serializeNulls()
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()
    private val tipoMapa = object : TypeToken<Map<String, Any?>>() {}.type

    /** Room ya es la copia local: Firestore sin caché en disco, solo memoria. */
    private val db by lazy {
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
        }
    }

    /** Las reglas de Firestore exigen sesión: se entra en forma anónima, sin pedirle nada al usuario. */
    private suspend fun asegurarSesion() {
        val auth = Firebase.auth
        if (auth.currentUser == null) auth.signInAnonymously().await()
    }

    suspend fun enviar(coleccion: String, id: String, registro: Any) {
        asegurarSesion()
        val datos = aMapa(registro) - "pendienteSync" + (TS to FieldValue.serverTimestamp())
        // Sin señal la tarea no termina nunca: cortamos y WorkManager reintenta.
        withTimeout(TIEMPO_MAX_MS) { db.collection(coleccion).document(id).set(datos).await() }
    }

    suspend fun descargar(desde: Long): PaqueteSync {
        asegurarSesion()
        var maximo = desde
        suspend fun <T> bajar(coleccion: String, clase: Class<T>): List<T> {
            var consulta: Query = db.collection(coleccion)
            if (desde > 0) consulta = consulta.whereGreaterThan(TS, Timestamp(java.util.Date(desde - SOLAPE_MS)))
            val docs = withTimeout(TIEMPO_MAX_MS) { consulta.get(Source.SERVER).await() }.documents
            return docs.mapNotNull { doc ->
                val datos = doc.data ?: return@mapNotNull null
                doc.getTimestamp(TS)?.toDate()?.time?.let { if (it > maximo) maximo = it }
                gson.fromJson(gson.toJsonTree(datos - TS), clase)
            }
        }
        val paquete = PaqueteSync(
            horaServidor = 0,
            tiposVehiculo = bajar(Colecciones.TIPOS_VEHICULO, TipoVehiculo::class.java),
            tiposCombustible = bajar(Colecciones.TIPOS_COMBUSTIBLE, TipoCombustible::class.java),
            operadores = bajar(Colecciones.OPERADORES, Operador::class.java),
            vehiculos = bajar(Colecciones.VEHICULOS, Vehiculo::class.java),
            tanques = bajar(Colecciones.TANQUES, Tanque::class.java),
            unidades = bajar(Colecciones.UNIDADES, Unidad::class.java),
            rutas = bajar(Colecciones.RUTAS, Ruta::class.java),
            viajes = bajar(Colecciones.VIAJES, Viaje::class.java),
            programaciones = bajar(Colecciones.PROGRAMACIONES, Programacion::class.java)
        )
        return paquete.copy(horaServidor = maximo)
    }

    /** true si Firestore todavía no tiene catálogos (proyecto recién creado). */
    suspend fun estaVacio(): Boolean {
        asegurarSesion()
        val docs = withTimeout(TIEMPO_MAX_MS) {
            db.collection(Colecciones.OPERADORES).limit(1).get(Source.SERVER).await()
        }
        return docs.isEmpty
    }

    /** Sube un paquete completo (lo usa la carga inicial de datos de prueba). */
    suspend fun subirTodo(p: PaqueteSync) {
        asegurarSesion()
        val lote = db.batch()
        fun poner(coleccion: String, id: String, registro: Any) {
            val datos = aMapa(registro) - "pendienteSync" + (TS to FieldValue.serverTimestamp())
            lote.set(db.collection(coleccion).document(id), datos)
        }
        p.tiposVehiculo.forEach { poner(Colecciones.TIPOS_VEHICULO, it.id, it) }
        p.tiposCombustible.forEach { poner(Colecciones.TIPOS_COMBUSTIBLE, it.id, it) }
        p.operadores.forEach { poner(Colecciones.OPERADORES, it.id, it) }
        p.vehiculos.forEach { poner(Colecciones.VEHICULOS, it.id, it) }
        p.tanques.forEach { poner(Colecciones.TANQUES, it.id, it) }
        p.unidades.forEach { poner(Colecciones.UNIDADES, it.id, it) }
        p.rutas.forEach { poner(Colecciones.RUTAS, it.id, it) }
        p.viajes.forEach { poner(Colecciones.VIAJES, it.id, it) }
        p.programaciones.forEach { poner(Colecciones.PROGRAMACIONES, it.id, it) }
        withTimeout(TIEMPO_MAX_MS) { lote.commit().await() }
    }

    private fun aMapa(registro: Any): Map<String, Any?> = gson.fromJson(gson.toJson(registro), tipoMapa)
}
