package com.flota.despacho.ui.operador

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flota.despacho.data.FlotaRepository
import com.flota.despacho.data.Momento
import com.flota.despacho.data.ResultadoPunto
import com.flota.despacho.data.db.Averia
import com.flota.despacho.data.db.Inspeccion
import com.flota.despacho.data.db.ViajeDetalle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class OperadorUi(
    val cargando: Boolean = true,
    /** Ya salió: le falta la llegada o la entrega. */
    val viajeAbierto: ViajeDetalle? = null,
    /** Programado por la oficina, todavía no salió. */
    val proximo: ViajeDetalle? = null,
    /** El último que entregó (para "Viaje terminado"). */
    val terminado: ViajeDetalle? = null,
    /** Sus checklists hechos (para saber si ya hizo el de salida o el de llegada). */
    val inspecciones: List<Inspeccion> = emptyList(),
    /** Averías graves sin resolver de toda la flota (bloquean la salida). */
    val averiasGraves: List<Averia> = emptyList()
)

class OperadorViewModel(private val repo: FlotaRepository, private val idOperador: String) : ViewModel() {

    val ui: StateFlow<OperadorUi> = combine(
        repo.viajeAbierto(idOperador),
        repo.proximoViaje(idOperador),
        repo.ultimoTerminado(idOperador),
        repo.inspeccionesDe(idOperador),
        repo.averiasGravesAbiertas()
    ) { abierto, proximo, terminado, inspecciones, graves ->
        OperadorUi(false, abierto, proximo, terminado, inspecciones, graves)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperadorUi())

    val pendientes: StateFlow<Int> = repo.pendientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var nombre by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // ---------- Borrador del checklist: vive aquí para no perderlo al girar la pantalla ----------
    val respuestas = mutableStateMapOf<String, ResultadoPunto>()
    var kilometraje by mutableStateOf("")
    var observaciones by mutableStateOf("")
    var errorChecklist by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch { nombre = repo.operador(idOperador)?.nombre.orEmpty() }
        repo.pedirSincronizacion() // traer viajes programados nuevos al abrir
    }

    fun limpiarError() { error = null }

    private fun registrar(accion: suspend () -> String?, onListo: () -> Unit) {
        viewModelScope.launch {
            error = accion()
            if (error == null) onListo()
        }
    }

    fun registrarSalida(idViaje: String, hora: Long, onListo: () -> Unit) =
        registrar({ repo.registrarSalida(idViaje, hora) }, onListo)

    fun registrarLlegada(idViaje: String, hora: Long, onListo: () -> Unit) =
        registrar({ repo.registrarLlegada(idViaje, hora) }, onListo)

    fun registrarEntrega(idViaje: String, hora: Long, onListo: () -> Unit) =
        registrar({ repo.registrarEntrega(idViaje, hora) }, onListo)

    fun responder(r: ResultadoPunto) { respuestas[r.codigo] = r; errorChecklist = null }

    fun nuevoChecklist() {
        respuestas.clear(); kilometraje = ""; observaciones = ""; errorChecklist = null
    }

    /** De qué viaje y momento es el borrador: si vuelve al mismo checklist, sigue donde lo dejó. */
    private var borradorDe: String? = null

    fun empezarChecklist(idViaje: String, momento: Momento) {
        val clave = "$idViaje-${momento.name}"
        if (borradorDe != clave) { nuevoChecklist(); borradorDe = clave }
    }

    /** [onListo] recibe cuántas averías se reportaron. */
    fun guardarChecklist(idViaje: String, momento: Momento, onListo: (Int) -> Unit) {
        val km = kilometraje.toIntOrNull()
        if (km == null) { errorChecklist = "Escribe el kilometraje."; return }
        viewModelScope.launch {
            val lista = respuestas.values.toList()
            errorChecklist = repo.guardarChecklist(idViaje, momento, km, lista, observaciones)
            if (errorChecklist == null) {
                onListo(lista.count { it.resultado == "M" })
                nuevoChecklist()
            }
        }
    }
}
