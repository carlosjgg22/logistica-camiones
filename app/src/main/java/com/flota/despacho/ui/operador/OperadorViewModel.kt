package com.flota.despacho.ui.operador

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flota.despacho.data.FlotaRepository
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
    val proximo: ViajeDetalle? = null
)

class OperadorViewModel(private val repo: FlotaRepository, private val idOperador: String) : ViewModel() {

    val ui: StateFlow<OperadorUi> = combine(
        repo.viajeAbierto(idOperador),
        repo.proximoViaje(idOperador)
    ) { abierto, proximo ->
        OperadorUi(cargando = false, viajeAbierto = abierto, proximo = proximo)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OperadorUi())

    val pendientes: StateFlow<Int> = repo.pendientes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    var nombre by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
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
}
