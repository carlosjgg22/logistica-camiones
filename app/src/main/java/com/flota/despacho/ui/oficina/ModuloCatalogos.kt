package com.flota.despacho.ui.oficina

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PropaneTank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flota.despacho.data.db.*
import com.flota.despacho.ui.componentes.*
import com.flota.despacho.ui.theme.colorDisponible
import com.flota.despacho.ui.theme.colorOcupado

/**
 * Lista + formulario de alta/edición, igual para rutas, tanques y vehículos.
 * [formulario] recibe el registro a editar (null = nuevo) y la acción para cerrar.
 */
@Composable
private fun <T> Catalogo(
    titulo: String,
    singular: String,
    icono: ImageVector,
    lista: List<T>,
    clave: (T) -> String,
    snackbar: SnackbarHostState,
    onVolver: () -> Unit,
    avisar: (String) -> Unit,
    tarjeta: @Composable (T) -> Unit,
    formulario: @Composable (actual: T?, onListo: () -> Unit) -> Unit
) {
    var edicion by remember { mutableStateOf<Edicion<T>?>(null) }
    BackHandler(enabled = edicion != null) { edicion = null }

    val e = edicion
    if (e != null) {
        val nuevo = e.item == null
        PantallaModulo(if (nuevo) "Nuevo $singular" else "Editar $singular", snackbar, onVolver = { edicion = null }) {
            formulario(e.item) {
                edicion = null
                avisar(if (nuevo) "Agregado" else "Cambios guardados")
            }
        }
        return
    }

    PantallaModulo(titulo, snackbar, onVolver, "Agregar $singular", onAgregar = { edicion = Edicion(null) }) {
        if (lista.isEmpty()) {
            EstadoVacio(icono, "Todavía no hay ${titulo.lowercase()}", "Toca \"Agregar $singular\" para empezar.")
        } else {
            LazyColumn(contentPadding = FinDeLista, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(lista, key = clave) { item ->
                    Card(onClick = { edicion = Edicion(item) }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { tarjeta(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Titulo(texto: String, enUnidad: Boolean? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        when (enUnidad) {
            true -> ChipTexto("En unidad", colorOcupado())
            false -> ChipTexto("Libre", colorDisponible())
            null -> {}
        }
    }
}

// ---------------- Rutas ----------------

@Composable
fun ModuloRutas(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.rutas.collectAsStateWithLifecycle()
    Catalogo(
        Modulo.RUTAS.titulo, "ruta", Icons.Filled.Map, lista, { it.id }, snackbar, onVolver, avisar,
        tarjeta = { r ->
            Titulo("Despacho ${r.numeroDespacho}")
            Text(r.recorrido, style = MaterialTheme.typography.bodyLarge)
            Secundario("Factura ${r.numeroFactura}")
        },
        formulario = { actual, onListo ->
            var despacho by rememberSaveable { mutableStateOf(actual?.numeroDespacho.orEmpty()) }
            var recorrido by rememberSaveable { mutableStateOf(actual?.recorrido.orEmpty()) }
            var factura by rememberSaveable { mutableStateOf(actual?.numeroFactura.orEmpty()) }
            var error by remember { mutableStateOf<String?>(null) }
            val valido = despacho.isNotBlank() && recorrido.isNotBlank() && factura.isNotBlank()

            Formulario(error, if (actual == null) "Agregar ruta" else "Guardar cambios", valido, onGuardar = {
                val r = Ruta(
                    id = actual?.id ?: nuevoId(),
                    numeroDespacho = despacho.trim(), recorrido = recorrido.trim(), numeroFactura = factura.trim()
                )
                vm.guardarRuta(r) { err -> error = err; if (err == null) onListo() }
            }) {
                CampoTexto("Número de despacho", despacho, { despacho = it; error = null })
                CampoTexto("Recorrido", recorrido, { recorrido = it; error = null })
                CampoTexto("Número de factura", factura, { factura = it; error = null })
            }
        }
    )
}

// ---------------- Tanques ----------------

@Composable
fun ModuloTanques(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.tanques.collectAsStateWithLifecycle()
    val combustibles by vm.combustibles.collectAsStateWithLifecycle()
    Catalogo(
        Modulo.TANQUES.titulo, "tanque", Icons.Filled.PropaneTank, lista, { it.id }, snackbar, onVolver, avisar,
        tarjeta = { t ->
            Titulo("Tanque ${t.matricula}", t.enUnidad)
            Text("${litros(t.volumenLitros)}, ${cantidadCompartimientos(t.compartimientos)}", style = MaterialTheme.typography.bodyLarge)
            Secundario(t.combustible)
        },
        formulario = { actual, onListo ->
            var matricula by rememberSaveable { mutableStateOf(actual?.matricula.orEmpty()) }
            var volumen by rememberSaveable { mutableStateOf(actual?.volumenLitros?.toString().orEmpty()) }
            var compartimientos by rememberSaveable { mutableStateOf(actual?.compartimientos?.toString().orEmpty()) }
            var idCombustible by rememberSaveable { mutableStateOf(actual?.idTipoCombustible) }
            val combustible = combustibles.find { it.id == idCombustible }
            var error by remember { mutableStateOf<String?>(null) }
            val vol = volumen.toIntOrNull()
            val comp = compartimientos.toIntOrNull()
            val valido = matricula.isNotBlank() && vol != null && vol > 0 && comp != null && comp > 0 && combustible != null

            Formulario(error, if (actual == null) "Agregar tanque" else "Guardar cambios", valido, onGuardar = {
                val c = combustible
                if (vol != null && comp != null && c != null) {
                    val t = Tanque(
                        id = actual?.id ?: nuevoId(),
                        matricula = matricula.trim().uppercase(), volumenLitros = vol, compartimientos = comp, idTipoCombustible = c.id
                    )
                    vm.guardarTanque(t) { err -> error = err; if (err == null) onListo() }
                }
            }) {
                CampoTexto("Matrícula", matricula, { matricula = it; error = null })
                CampoTexto("Volumen", volumen, { volumen = it; error = null }, numerico = true, sufijo = "L")
                CampoTexto("Compartimientos", compartimientos, { compartimientos = it; error = null }, numerico = true)
                Selector("Tipo de combustible", combustibles, combustible, { it.nombre }) { idCombustible = it.id; error = null }
            }
        }
    )
}

private fun cantidadCompartimientos(n: Int) = if (n == 1) "1 compartimiento" else "$n compartimientos"

// ---------------- Vehículos ----------------

@Composable
fun ModuloVehiculos(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.vehiculos.collectAsStateWithLifecycle()
    val tipos by vm.tiposVehiculo.collectAsStateWithLifecycle()
    Catalogo(
        Modulo.VEHICULOS.titulo, "vehículo", Icons.Filled.LocalShipping, lista, { it.id }, snackbar, onVolver, avisar,
        tarjeta = { v ->
            Titulo("Vehículo ${v.matricula}", v.enUnidad)
            Text(v.descripcion, style = MaterialTheme.typography.bodyLarge)
            Secundario(v.tipo)
        },
        formulario = { actual, onListo ->
            var matricula by rememberSaveable { mutableStateOf(actual?.matricula.orEmpty()) }
            var descripcion by rememberSaveable { mutableStateOf(actual?.descripcion.orEmpty()) }
            var idTipo by rememberSaveable { mutableStateOf(actual?.idTipoVehiculo) }
            val tipo = tipos.find { it.id == idTipo }
            var error by remember { mutableStateOf<String?>(null) }
            val valido = matricula.isNotBlank() && descripcion.isNotBlank() && tipo != null

            Formulario(error, if (actual == null) "Agregar vehículo" else "Guardar cambios", valido, onGuardar = {
                val t = tipo
                if (t != null) {
                    val v = Vehiculo(
                        id = actual?.id ?: nuevoId(),
                        matricula = matricula.trim().uppercase(), descripcion = descripcion.trim(), idTipoVehiculo = t.id
                    )
                    vm.guardarVehiculo(v) { err -> error = err; if (err == null) onListo() }
                }
            }) {
                CampoTexto("Matrícula", matricula, { matricula = it; error = null })
                CampoTexto("Descripción", descripcion, { descripcion = it; error = null })
                Selector("Tipo de vehículo", tipos, tipo, { it.nombre }) { idTipo = it.id; error = null }
            }
        }
    )
}
