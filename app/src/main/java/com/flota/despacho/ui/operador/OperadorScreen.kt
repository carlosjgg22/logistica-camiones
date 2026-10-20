package com.flota.despacho.ui.operador

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flota.despacho.FlotaApp
import com.flota.despacho.R
import com.flota.despacho.data.db.ViajeDetalle
import com.flota.despacho.ui.componentes.*
import com.flota.despacho.ui.theme.colorDisponible
import kotlinx.coroutines.launch

/** "Viaje terminado" se muestra hasta un día después de la entrega; después, el estado vacío. */
private const val MOSTRAR_TERMINADO_MS = 24 * 60 * 60 * 1000L

/** El paso que le toca registrar al operador, con lo que se muestra en el pie fijo. */
private enum class Paso(val titulo: String, val etiquetaHora: String, @DrawableRes val icono: Int, val secundario: Boolean) {
    SALIDA("Registrar salida", "Hora de salida", R.drawable.ic_local_shipping_fill, true),
    LLEGADA("Registrar llegada", "Hora de llegada", R.drawable.ic_flag_fill, false),
    ENTREGA("Registrar entrega del producto", "Hora de entrega", R.drawable.ic_inventory_2_fill, false)
}

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
    val ahora = rememberAhora(30_000)

    // El viaje en pantalla y el paso que le falta: salida, llegada o entrega
    val abierto = ui.viajeAbierto
    val viaje = abierto ?: ui.proximo
    val paso = when {
        abierto != null && abierto.fechaLlegada == null -> Paso.LLEGADA
        abierto != null -> Paso.ENTREGA
        ui.proximo != null -> Paso.SALIDA
        else -> null
    }
    val terminado = ui.terminado?.takeIf { t -> t.fechaEntrega?.let { ahora - it < MOSTRAR_TERMINADO_MS } == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.nombre.isBlank()) "Mi viaje" else "Hola, ${vm.nombre}") },
                actions = {
                    IconButton(onClick = { confirmarCierre = true }) {
                        Icon(painterResource(R.drawable.ic_logout), contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        // Pie fijo: el botón del paso siempre se ve, aunque la tarjeta del viaje haga scroll
        bottomBar = {
            if (viaje != null && paso != null) {
                PieDePaso(paso, vm.error, vm::limpiarError) { h ->
                    when (paso) {
                        Paso.SALIDA -> vm.registrarSalida(viaje.id, h) { avisar("Salida registrada. Buen viaje.") }
                        Paso.LLEGADA -> vm.registrarLlegada(viaje.id, h) {
                            avisar("Llegada registrada. Viaje de ${duracion(h - (viaje.fechaSalida ?: h))}.")
                        }
                        Paso.ENTREGA -> vm.registrarEntrega(viaje.id, h) { avisar("Entrega registrada. Viaje terminado.") }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AvisoSync(pendientes)
            when {
                ui.cargando -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                viaje != null -> TarjetaViaje(viaje, ahora)
                terminado != null -> ViajeTerminado(terminado)
                else -> EstadoVacio(
                    R.drawable.ic_event_busy,
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

/** "Diésel · 30.000 L en 3 compartimientos", con los datos del tanque de la unidad. */
private fun ViajeDetalle.textoCarga(): String? {
    val partes = listOfNotNull(
        carga,
        volumenLitros?.let { l ->
            litros(l) + (compartimientos?.let { if (it == 1) " en 1 compartimiento" else " en $it compartimientos" } ?: "")
        }
    )
    return partes.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Composable
private fun TarjetaViaje(v: ViajeDetalle, ahora: Long) {
    // En pantallas bajas se oculta la ruta para que entre todo sin scroll
    val pantallaAlta = LocalConfiguration.current.screenHeightDp >= 760
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Despacho ${v.numeroDespacho}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ChipEstado(v.estado)
            }
            Dato(R.drawable.ic_place, "Destino", v.destino, destacado = true)
            if (pantallaAlta) Dato(R.drawable.ic_map, "Ruta", v.recorrido)
            if (v.vehiculo != null) Dato(R.drawable.ic_local_shipping, "Unidad", "Vehículo ${v.vehiculo} con tanque ${v.tanque}")
            v.textoCarga()?.let { Dato(R.drawable.ic_local_gas_station, "Carga", it) }
            val salida = v.fechaSalida
            val llegada = v.fechaLlegada
            when {
                salida == null -> v.fechaProgramada?.let { Dato(R.drawable.ic_calendar_today, "Fecha", fecha(it)) }
                llegada == null -> Dato(R.drawable.ic_timer, "Tiempo en ruta", duracion(ahora - salida))
                else -> Dato(R.drawable.ic_timer, "Duración", duracion(llegada - salida))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Seguimiento(v)
        }
    }
}

/** Salida · Llegada · Entrega: verde si ya está, azul el que toca, gris lo pendiente. */
@Composable
private fun Seguimiento(v: ViajeDetalle) {
    val horas = listOf("Salida" to v.fechaSalida, "Llegada" to v.fechaLlegada, "Entrega" to v.fechaEntrega)
    val actual = horas.indexOfFirst { it.second == null }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        horas.forEachIndexed { i, (etiqueta, valor) ->
            val hecho = valor != null
            val color: Color = when {
                hecho -> colorDisponible()
                i == actual -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
                Text(
                    etiqueta,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (hecho || i == actual) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    valor?.let(::hora) ?: "—",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = if (hecho) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ViajeTerminado(v: ViajeDetalle) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_task_alt_fill), contentDescription = null,
            tint = colorDisponible(), modifier = Modifier.size(64.dp)
        )
        Text("Viaje terminado", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        val detalle = listOfNotNull(
            v.duracionMs?.let { "Duración ${duracion(it)}" },
            v.fechaEntrega?.let { "entrega a las ${hora(it)}" }
        ).joinToString(" · ")
        Text(
            "Despacho ${v.numeroDespacho} a ${v.destino}." + if (detalle.isNotEmpty()) " $detalle" else "",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            "Cuando la oficina te programe otro viaje aparecerá aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/** Pie fijo de un paso: título, hora (ahora o corregida) y botón, con confirmación. */
@Composable
private fun PieDePaso(paso: Paso, error: String?, onCambio: () -> Unit, onRegistrar: (Long) -> Unit) {
    var elegida by remember(paso) { mutableStateOf<Long?>(null) }
    var confirmar by remember { mutableStateOf(false) }

    Column(
        Modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(paso.titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CampoHora(paso.etiquetaHora, elegida) { elegida = it; onCambio() }
        if (error != null) MensajeError(error)
        BotonGrande(paso.titulo, onClick = { confirmar = true }, icono = paso.icono, secundario = paso.secundario)
    }

    if (confirmar) {
        val horaTexto = elegida?.let { hora(it) } ?: "la hora actual"
        Confirmacion(
            titulo = "¿${paso.titulo}?",
            texto = "${paso.etiquetaHora}: $horaTexto.",
            boton = paso.titulo,
            onConfirmar = {
                confirmar = false
                onRegistrar(elegida ?: System.currentTimeMillis())
            },
            onCancelar = { confirmar = false }
        )
    }
}
