package com.flota.despacho.ui.oficina

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flota.despacho.FlotaApp
import com.flota.despacho.ui.componentes.*
import kotlinx.coroutines.launch

enum class Modulo(val titulo: String, val icono: ImageVector) {
    OPERADORES("Operadores", Icons.Filled.Groups),
    VIAJES("Viajes", Icons.Filled.Route),
    PROGRAMACION("Programación", Icons.Filled.EditCalendar),
    UNIDADES("Unidades", Icons.Filled.Hub),
    RUTAS("Rutas", Icons.Filled.Map),
    TANQUES("Tanques", Icons.Filled.PropaneTank),
    VEHICULOS("Vehículos", Icons.Filled.LocalShipping)
}

@Composable
fun OficinaScreen(app: FlotaApp, onSalir: () -> Unit) {
    val vm: OficinaViewModel = viewModel(factory = viewModelFactory { initializer { OficinaViewModel(app.repo) } })
    var modulo by rememberSaveable { mutableStateOf<Modulo?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisar: (String) -> Unit = { texto -> scope.launch { snackbar.showSnackbar(texto) } }
    val volver = { modulo = null }

    BackHandler(enabled = modulo != null) { volver() }

    when (modulo) {
        null -> Menu(vm, snackbar, onAbrir = { modulo = it }, onSalir = onSalir, avisar = avisar)
        Modulo.OPERADORES -> ModuloOperadores(vm, snackbar, volver, avisar)
        Modulo.VIAJES -> ModuloViajes(vm, snackbar, volver, avisar)
        Modulo.PROGRAMACION -> ModuloProgramacion(vm, snackbar, volver, avisar)
        Modulo.UNIDADES -> ModuloUnidades(vm, snackbar, volver, avisar)
        Modulo.RUTAS -> ModuloRutas(vm, snackbar, volver, avisar)
        Modulo.TANQUES -> ModuloTanques(vm, snackbar, volver, avisar)
        Modulo.VEHICULOS -> ModuloVehiculos(vm, snackbar, volver, avisar)
    }
}

// ---------------- Menú de íconos ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Menu(
    vm: OficinaViewModel,
    snackbar: SnackbarHostState,
    onAbrir: (Modulo) -> Unit,
    onSalir: () -> Unit,
    avisar: (String) -> Unit
) {
    val ultimaSync by vm.ultimaSync.collectAsStateWithLifecycle()
    val operadores by vm.operadores.collectAsStateWithLifecycle()
    val viajes by vm.viajes.collectAsStateWithLifecycle()
    val enCurso by vm.viajesEnCurso.collectAsStateWithLifecycle()
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val rutas by vm.rutas.collectAsStateWithLifecycle()
    val tanques by vm.tanques.collectAsStateWithLifecycle()
    val vehiculos by vm.vehiculos.collectAsStateWithLifecycle()

    fun resumen(m: Modulo): String = when (m) {
        Modulo.OPERADORES -> cantidad(operadores.size, "operador", "operadores")
        Modulo.VIAJES -> cantidad(viajes.size, "viaje", "viajes")
        Modulo.PROGRAMACION -> "${enCurso.size} en curso"
        Modulo.UNIDADES -> cantidad(unidades.size, "unidad", "unidades")
        Modulo.RUTAS -> cantidad(rutas.size, "ruta", "rutas")
        Modulo.TANQUES -> cantidad(tanques.size, "tanque", "tanques")
        Modulo.VEHICULOS -> cantidad(vehiculos.size, "vehículo", "vehículos")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Oficina") },
                actions = {
                    IconButton(onClick = { vm.actualizar(); avisar("Actualizando…") }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                    IconButton(onClick = onSalir) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            BarraActualizacion(vm.servidorConfigurado, ultimaSync)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(Modulo.entries.toList(), key = { it.name }) { m ->
                    TarjetaModulo(m, resumen(m)) { onAbrir(m) }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

private fun cantidad(n: Int, uno: String, varios: String) = "$n ${if (n == 1) uno else varios}"

@Composable
private fun TarjetaModulo(m: Modulo, resumen: String, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(m.icono, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                }
            }
            Text(m.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Secundario(resumen)
        }
    }
}

@Composable
private fun BarraActualizacion(servidor: Boolean, ultimaSync: Long?) {
    val texto = when {
        !servidor -> "Sin servidor configurado: se muestran solo los datos de este teléfono."
        ultimaSync == null -> "Todavía no se descargaron datos. Revisa la conexión."
        else -> "Datos actualizados a las ${hora(ultimaSync)}"
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ---------------- Piezas comunes de los módulos ----------------

/**
 * Pantalla de un módulo: barra con flecha para volver y botón flotante para agregar.
 * Sin [onAgregar] no se muestra el botón (por ejemplo, mientras está abierto el formulario).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PantallaModulo(
    titulo: String,
    snackbar: SnackbarHostState,
    onVolver: () -> Unit,
    textoAgregar: String? = null,
    onAgregar: (() -> Unit)? = null,
    contenido: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            if (onAgregar != null && textoAgregar != null) {
                ExtendedFloatingActionButton(
                    onClick = onAgregar,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(textoAgregar) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) { contenido() }
    }
}

/** Formulario de alta o edición, con el botón de guardar al final. */
@Composable
internal fun Formulario(
    error: String?,
    textoBoton: String,
    habilitado: Boolean,
    onGuardar: () -> Unit,
    campos: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        campos()
        if (error != null) MensajeError(error)
        BotonGrande(textoBoton, onClick = onGuardar, enabled = habilitado)
    }
}

/** Estado de un módulo con lista + formulario: null = viendo la lista. */
internal class Edicion<T>(val item: T?)

/** Espacio al final de las listas para que el botón flotante no tape la última tarjeta. */
internal val FinDeLista = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp)
