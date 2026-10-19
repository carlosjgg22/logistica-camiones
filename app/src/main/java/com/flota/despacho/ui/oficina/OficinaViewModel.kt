package com.flota.despacho.ui.oficina

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flota.despacho.data.FlotaRepository
import com.flota.despacho.data.db.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OficinaViewModel(private val repo: FlotaRepository) : ViewModel() {

    private fun <T> Flow<T>.estado(inicial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), inicial)

    val operadores: StateFlow<List<OperadorEstado>> = repo.operadores().estado(emptyList())
    val viajes: StateFlow<List<ViajeDetalle>> = repo.viajes().estado(emptyList())
    val viajesEnCurso: StateFlow<List<ViajeDetalle>> = repo.viajesEnCurso().estado(emptyList())
    val unidades: StateFlow<List<UnidadDetalle>> = repo.unidades().estado(emptyList())
    val vehiculos: StateFlow<List<VehiculoDetalle>> = repo.vehiculos().estado(emptyList())
    val tanques: StateFlow<List<TanqueDetalle>> = repo.tanques().estado(emptyList())
    val rutas: StateFlow<List<Ruta>> = repo.rutas().estado(emptyList())
    val tiposVehiculo: StateFlow<List<TipoVehiculo>> = repo.tiposVehiculo().estado(emptyList())
    val combustibles: StateFlow<List<TipoCombustible>> = repo.combustibles().estado(emptyList())
    val ultimaSync: StateFlow<Long?> = repo.ultimaSync
    val servidorConfigurado: Boolean = repo.servidorConfigurado

    init { actualizar() }

    fun actualizar() = repo.pedirSincronizacion()

    /** Corre el guardado y avisa: null si salió bien, o el mensaje de error. */
    private fun guardar(accion: suspend () -> String?, onListo: (String?) -> Unit) {
        viewModelScope.launch { onListo(accion()) }
    }

    fun guardarOperador(o: Operador, onListo: (String?) -> Unit) = guardar({ repo.guardarOperador(o) }, onListo)
    fun guardarVehiculo(v: Vehiculo, onListo: (String?) -> Unit) = guardar({ repo.guardarVehiculo(v) }, onListo)
    fun guardarTanque(t: Tanque, onListo: (String?) -> Unit) = guardar({ repo.guardarTanque(t) }, onListo)
    fun guardarRuta(r: Ruta, onListo: (String?) -> Unit) = guardar({ repo.guardarRuta(r) }, onListo)
    fun guardarViaje(v: Viaje, onListo: (String?) -> Unit) = guardar({ repo.guardarViaje(v) }, onListo)

    fun conformarUnidad(idVehiculo: String, idTanque: String, idOperador: String, onListo: (String?) -> Unit) =
        guardar({ repo.conformarUnidad(idVehiculo, idTanque, idOperador) }, onListo)

    fun deshacerUnidad(id: String, onListo: (String?) -> Unit) = guardar({ repo.deshacerUnidad(id) }, onListo)

    fun programarViaje(idViaje: String, idUnidad: String, idCombustible: String, fecha: Long, onListo: (String?) -> Unit) =
        guardar({ repo.programarViaje(idViaje, idUnidad, idCombustible, fecha) }, onListo)
}
