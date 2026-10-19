package com.flota.despacho.ui.oficina

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flota.despacho.data.db.*
import com.flota.despacho.ui.componentes.*
import com.flota.despacho.ui.theme.colorDisponible
import com.flota.despacho.ui.theme.colorOcupado

fun UnidadDetalle.texto() = "Vehículo $vehiculo + tanque $tanque ($operador)"

@Composable
fun ModuloUnidades(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.unidades.collectAsStateWithLifecycle()
    var conformando by rememberSaveable { mutableStateOf(false) }
    var deshacer by remember { mutableStateOf<UnidadDetalle?>(null) }
    BackHandler(enabled = conformando) { conformando = false }

    if (conformando) {
        PantallaModulo("Conformar unidad", snackbar, onVolver = { conformando = false }) {
            FormularioUnidad(vm) {
                conformando = false
                avisar("Unidad conformada. Ya se puede asignar a un viaje.")
            }
        }
        return
    }

    PantallaModulo(Modulo.UNIDADES.titulo, snackbar, onVolver, "Conformar unidad", onAgregar = { conformando = true }) {
        LazyColumn(contentPadding = FinDeLista, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Secundario("Una unidad es un vehículo con un tanque y un operador. Se asigna a los viajes al programarlos.") }
            if (lista.isEmpty()) {
                item { EstadoVacio(Icons.Filled.Hub, "No hay unidades", "Toca \"Conformar unidad\" para armar la primera.") }
            }
            items(lista, key = { it.id }) { u -> TarjetaUnidad(u) { deshacer = u } }
        }
    }

    val d = deshacer
    if (d != null) {
        Confirmacion(
            titulo = "¿Deshacer la unidad?",
            texto = "${d.texto()}. El vehículo, el tanque y el operador quedan libres para armar otra unidad.",
            boton = "Deshacer",
            onConfirmar = {
                deshacer = null
                vm.deshacerUnidad(d.id) { err -> avisar(err ?: "Unidad deshecha") }
            },
            onCancelar = { deshacer = null }
        )
    }
}

@Composable
private fun TarjetaUnidad(u: UnidadDetalle, onDeshacer: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Unidad ${u.vehiculo}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (u.enUso) ChipTexto("En ruta", colorOcupado()) else ChipTexto("Disponible", colorDisponible())
            }
            Dato(Icons.Filled.LocalShipping, "Vehículo", "${u.vehiculo}, ${u.descripcionVehiculo}")
            Dato(Icons.Filled.PropaneTank, "Tanque", "${u.tanque}, ${litros(u.volumenLitros)}")
            Dato(Icons.Filled.LocalGasStation, "Combustible", u.combustible)
            Dato(Icons.Filled.Person, "Operador", u.operador)
            if (!u.enUso) {
                TextButton(onClick = onDeshacer, contentPadding = PaddingValues(horizontal = 0.dp)) {
                    Icon(Icons.Filled.LinkOff, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Deshacer unidad")
                }
            }
        }
    }
}

@Composable
private fun FormularioUnidad(vm: OficinaViewModel, onListo: () -> Unit) {
    // Solo se ofrece lo que no está en otra unidad
    val vehiculos by vm.vehiculos.collectAsStateWithLifecycle()
    val tanques by vm.tanques.collectAsStateWithLifecycle()
    val operadores by vm.operadores.collectAsStateWithLifecycle()
    val vehiculosLibres = vehiculos.filter { !it.enUnidad }
    val tanquesLibres = tanques.filter { !it.enUnidad }
    val operadoresLibres = operadores.filter { !it.enUnidad }

    var idVehiculo by rememberSaveable { mutableStateOf<String?>(null) }
    var idTanque by rememberSaveable { mutableStateOf<String?>(null) }
    var idOperador by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val vehiculo = vehiculosLibres.find { it.id == idVehiculo }
    val tanque = tanquesLibres.find { it.id == idTanque }
    val operador = operadoresLibres.find { it.id == idOperador }

    Formulario(error, "Conformar unidad", vehiculo != null && tanque != null && operador != null, onGuardar = {
        if (vehiculo != null && tanque != null && operador != null) {
            vm.conformarUnidad(vehiculo.id, tanque.id, operador.id) { err -> error = err; if (err == null) onListo() }
        }
    }) {
        val faltan = buildList {
            if (vehiculosLibres.isEmpty()) add("vehículos")
            if (tanquesLibres.isEmpty()) add("tanques")
            if (operadoresLibres.isEmpty()) add("operadores")
        }
        if (faltan.isNotEmpty()) {
            MensajeError("No hay ${faltan.joinToString(" ni ")} libres. Agrégalos en su módulo o deshaz otra unidad.")
        }
        Selector("Vehículo", vehiculosLibres, vehiculo, { "${it.matricula}, ${it.descripcion} (${it.tipo})" }) { idVehiculo = it.id; error = null }
        Selector("Tanque", tanquesLibres, tanque, { "${it.matricula}, ${litros(it.volumenLitros)} de ${it.combustible}" }) { idTanque = it.id; error = null }
        Selector("Operador", operadoresLibres, operador, { "${it.nombreCompleto}, cédula ${it.cedula}" }) { idOperador = it.id; error = null }
    }
}
