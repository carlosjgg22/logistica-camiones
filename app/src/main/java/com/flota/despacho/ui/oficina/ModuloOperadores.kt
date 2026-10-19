package com.flota.despacho.ui.oficina

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flota.despacho.data.db.*
import com.flota.despacho.ui.componentes.*
import com.flota.despacho.ui.theme.colorDisponible
import com.flota.despacho.ui.theme.colorOcupado

private enum class FiltroOperador(val texto: String) { TODOS("Todos"), DISPONIBLES("Disponibles"), EN_VIAJE("En viaje") }

@Composable
fun ModuloOperadores(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.operadores.collectAsStateWithLifecycle()
    var edicion by remember { mutableStateOf<Edicion<OperadorEstado>?>(null) }
    BackHandler(enabled = edicion != null) { edicion = null }

    val e = edicion
    if (e != null) {
        PantallaModulo(if (e.item == null) "Nuevo operador" else "Editar operador", snackbar, onVolver = { edicion = null }) {
            FormularioOperador(vm, e.item) { nuevo ->
                edicion = null
                avisar(if (nuevo) "Operador agregado" else "Operador actualizado")
            }
        }
        return
    }

    var filtro by rememberSaveable { mutableStateOf(FiltroOperador.TODOS) }
    val ahora = rememberAhora(30_000)
    val disponibles = lista.count { !it.enViaje }
    val visibles = when (filtro) {
        FiltroOperador.TODOS -> lista
        FiltroOperador.DISPONIBLES -> lista.filter { !it.enViaje }
        FiltroOperador.EN_VIAJE -> lista.filter { it.enViaje }
    }

    PantallaModulo(Modulo.OPERADORES.titulo, snackbar, onVolver, "Agregar operador", onAgregar = { edicion = Edicion(null) }) {
        LazyColumn(contentPadding = FinDeLista, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "$disponibles disponibles, ${lista.size - disponibles} en viaje",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FiltroOperador.entries.forEach { f ->
                            FilterChip(selected = filtro == f, onClick = { filtro = f }, label = { Text(f.texto) })
                        }
                    }
                }
            }
            if (visibles.isEmpty()) {
                item { EstadoVacio(Icons.Filled.Groups, "Nadie en esta lista", "Agrega un operador o prueba con otro filtro.") }
            }
            items(visibles, key = { it.id }) { o -> TarjetaOperador(o, ahora) { edicion = Edicion(o) } }
        }
    }
}

@Composable
private fun TarjetaOperador(o: OperadorEstado, ahora: Long, onEditar: () -> Unit) {
    val context = LocalContext.current
    Card(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(o.nombreCompleto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (o.enViaje) ChipTexto("En viaje", colorOcupado()) else ChipTexto("Disponible", colorDisponible())
            }
            Secundario("Cédula ${o.cedula}")
            Secundario(if (o.vehiculo != null) "En la unidad del vehículo ${o.vehiculo}" else "Sin unidad asignada")
            val salida = o.fechaSalida
            if (salida != null) {
                Secundario("Salió a las ${hora(salida)}" + (o.destino?.let { " rumbo a $it" } ?: "") + ", lleva ${duracion(ahora - salida)}")
            }
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${o.telefono}"))) },
                contentPadding = PaddingValues(horizontal = 0.dp)
            ) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Llamar al ${o.telefono}")
            }
        }
    }
}

/** [onListo] recibe true si fue un alta. */
@Composable
private fun FormularioOperador(vm: OficinaViewModel, actual: OperadorEstado?, onListo: (Boolean) -> Unit) {
    var nombre by rememberSaveable { mutableStateOf(actual?.nombre.orEmpty()) }
    var apellido by rememberSaveable { mutableStateOf(actual?.apellido.orEmpty()) }
    var telefono by rememberSaveable { mutableStateOf(actual?.telefono.orEmpty()) }
    var cedula by rememberSaveable { mutableStateOf(actual?.cedula.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }

    val valido = nombre.isNotBlank() && apellido.isNotBlank() && telefono.isNotBlank() && cedula.isNotBlank()

    Formulario(error, if (actual == null) "Agregar operador" else "Guardar cambios", valido, onGuardar = {
        val o = Operador(
            id = actual?.id ?: nuevoId(),
            nombre = nombre.trim(), apellido = apellido.trim(),
            telefono = telefono.trim(), cedula = cedula.trim()
        )
        vm.guardarOperador(o) { err -> error = err; if (err == null) onListo(actual == null) }
    }) {
        CampoTexto("Nombre", nombre, { nombre = it; error = null })
        CampoTexto("Apellido", apellido, { apellido = it; error = null })
        CampoTexto("Teléfono", telefono, { telefono = it; error = null }, numerico = true)
        CampoTexto("Cédula", cedula, { cedula = it; error = null }, numerico = true)
        Secundario("Con la cédula el operador entra a la app en su teléfono.")
    }
}
