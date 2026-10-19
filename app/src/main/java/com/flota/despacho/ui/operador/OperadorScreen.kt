package com.flota.despacho.ui.operador

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flota.despacho.FlotaApp
import com.flota.despacho.data.db.ViajeDetalle
import com.flota.despacho.ui.componentes.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperadorScreen(app: FlotaApp, idOperador: String, onSalir: () -> Unit) {
    val vm: OperadorViewModel = viewModel(
        key = "operador-$idOperador",
        factory = viewModelFactory { initializer { OperadorViewModel(app.repo, idOperador) } }
    )
    val ui by vm.ui.collectAsStateWithLifecycle()
    val pendientes by vm.pendientes.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var confirmarCierre by remember { mutableStateOf(false) }
    val avisar: (String) -> Unit = { texto -> scope.launch { snackbar.showSnackbar(texto) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.nombre.isBlank()) "Mi viaje" else "Hola, ${vm.nombre}") },
                actions = {
                    IconButton(onClick = { confirmarCierre = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AvisoSync(pendientes)
            val abierto = ui.viajeAbierto
            val proximo = ui.proximo
            when {
                ui.cargando -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                abierto != null && abierto.fechaLlegada == null -> {
                    TarjetaViaje(abierto)
                    Paso(
                        titulo = "Registrar llegada",
                        etiquetaHora = "Hora de llegada",
                        icono = Icons.Filled.Flag,
                        error = vm.error,
                        onCambio = vm::limpiarError
                    ) { h ->
                        vm.registrarLlegada(abierto.id, h) {
                            avisar("Llegada registrada. Viaje de ${duracion(h - (abierto.fechaSalida ?: h))}.")
                        }
                    }
                }
                abierto != null -> {
                    TarjetaViaje(abierto)
                    Paso(
                        titulo = "Registrar entrega del producto",
                        etiquetaHora = "Hora de entrega",
                        icono = Icons.Filled.Inventory2,
                        error = vm.error,
                        onCambio = vm::limpiarError
                    ) { h -> vm.registrarEntrega(abierto.id, h) { avisar("Entrega registrada. Viaje terminado.") } }
                }
                proximo != null -> {
                    TarjetaViaje(proximo)
                    Paso(
                        titulo = "Registrar salida",
                        etiquetaHora = "Hora de salida",
                        icono = Icons.Filled.LocalShipping,
                        secundario = true,
                        error = vm.error,
                        onCambio = vm::limpiarError
                    ) { h -> vm.registrarSalida(proximo.id, h) { avisar("Salida registrada. Buen viaje.") } }
                }
                else -> EstadoVacio(
                    Icons.Filled.EventBusy,
                    "No tienes viajes programados",
                    "Cuando la oficina te programe un viaje aparecerá aquí."
                )
            }
        }
    }

    if (confirmarCierre) {
        Confirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Para volver a entrar tendrás que escribir tu cédula.",
            boton = "Cerrar sesión",
            onConfirmar = { confirmarCierre = false; onSalir() },
            onCancelar = { confirmarCierre = false }
        )
    }
}

@Composable
private fun TarjetaViaje(v: ViajeDetalle) {
    val ahora = rememberAhora(30_000)
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Despacho ${v.numeroDespacho}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ChipEstado(v.estado)
            }
            Dato(Icons.Filled.Place, "Destino", v.destino, destacado = true)
            Dato(Icons.Filled.Map, "Ruta", v.recorrido)
            if (v.vehiculo != null) Dato(Icons.Filled.LocalShipping, "Unidad", "Vehículo ${v.vehiculo} con tanque ${v.tanque}")
            v.carga?.let { Dato(Icons.Filled.LocalGasStation, "Carga", it) }
            val salida = v.fechaSalida
            val llegada = v.fechaLlegada
            when {
                salida == null -> v.fechaProgramada?.let { Dato(Icons.Filled.CalendarToday, "Fecha", fecha(it)) }
                llegada == null -> {
                    Dato(Icons.Filled.Flag, "Salida", fechaHora(salida))
                    Dato(Icons.Filled.Timer, "Tiempo en ruta", duracion(ahora - salida))
                }
                else -> {
                    Dato(Icons.Filled.Flag, "Llegada", fechaHora(llegada))
                    Dato(Icons.Filled.Timer, "Duración", duracion(llegada - salida))
                }
            }
        }
    }
}

/** Un paso del viaje: elegir la hora (ahora o corregida) y confirmar. */
@Composable
private fun Paso(
    titulo: String,
    etiquetaHora: String,
    icono: ImageVector,
    error: String?,
    onCambio: () -> Unit,
    secundario: Boolean = false,
    onRegistrar: (Long) -> Unit
) {
    var elegida by remember(titulo) { mutableStateOf<Long?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    CampoHora(etiquetaHora, elegida) { elegida = it; onCambio() }
    if (error != null) MensajeError(error)
    BotonGrande(titulo, onClick = { confirmar = true }, icono = icono, secundario = secundario)

    if (confirmar) {
        val horaTexto = elegida?.let { hora(it) } ?: "la hora actual"
        Confirmacion(
            titulo = "¿$titulo?",
            texto = "$etiquetaHora: $horaTexto.",
            boton = titulo,
            onConfirmar = {
                confirmar = false
                onRegistrar(elegida ?: System.currentTimeMillis())
            },
            onCancelar = { confirmar = false }
        )
    }
}
