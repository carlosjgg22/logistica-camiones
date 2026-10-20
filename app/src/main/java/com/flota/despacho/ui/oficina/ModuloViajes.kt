package com.flota.despacho.ui.oficina

import com.flota.despacho.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

private fun Ruta.texto() = "Despacho $numeroDespacho, $recorrido"

@Composable
private fun TarjetaViaje(v: ViajeDetalle, ahora: Long, onClick: (() -> Unit)? = null) {
    val contenido: @Composable ColumnScope.() -> Unit = {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Despacho ${v.numeroDespacho}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            ChipEstado(v.estado)
        }
        Text("Destino: ${v.destino}", style = MaterialTheme.typography.bodyLarge)
        Secundario("Ruta: ${v.recorrido}. Factura ${v.numeroFactura}")
        Secundario(
            if (v.vehiculo != null) "Unidad ${v.vehiculo} + tanque ${v.tanque}" + (v.operador?.let { ", $it" } ?: "")
            else "Sin unidad asignada"
        )
        val programada = v.fechaProgramada
        if (programada != null) Secundario("Programado para el ${fecha(programada).lowercase()}" + (v.carga?.let { ", carga $it" } ?: ""))
        v.fechaSalida?.let { Secundario("Salida: ${fechaHora(it)}") }
        v.fechaLlegada?.let { Secundario("Llegada: ${fechaHora(it)}") }
        v.fechaEntrega?.let { Secundario("Entrega del producto: ${fechaHora(it)}") }
        val salida = v.fechaSalida
        when {
            v.duracionMs != null -> Secundario("Duración: ${duracion(v.duracionMs!!)}")
            salida != null -> Secundario("Lleva ${duracion(ahora - salida)} en ruta")
        }
    }
    if (onClick != null) {
        Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = contenido)
        }
    } else {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = contenido)
        }
    }
}

// ---------------- Viajes: alta, edición y fechas ----------------

@Composable
fun ModuloViajes(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val lista by vm.viajes.collectAsStateWithLifecycle()
    var edicion by remember { mutableStateOf<Edicion<ViajeDetalle>?>(null) }
    BackHandler(enabled = edicion != null) { edicion = null }

    val e = edicion
    if (e != null) {
        PantallaModulo(if (e.item == null) "Nuevo viaje" else "Editar viaje", snackbar, onVolver = { edicion = null }) {
            FormularioViaje(vm, e.item) {
                avisar(if (e.item == null) "Viaje agregado" else "Viaje actualizado")
                edicion = null
            }
        }
        return
    }

    var filtro by rememberSaveable { mutableStateOf<EstadoViaje?>(null) }
    val visibles = remember(lista, filtro) { lista.filter { filtro == null || it.estado == filtro } }
    val ahora = rememberAhora(30_000)

    PantallaModulo(Modulo.VIAJES.titulo, snackbar, onVolver, "Agregar viaje", onAgregar = { edicion = Edicion(null) }) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filtro == null, onClick = { filtro = null }, label = { Text("Todos (${lista.size})") })
                EstadoViaje.entries.forEach { est ->
                    FilterChip(
                        selected = filtro == est,
                        onClick = { filtro = if (filtro == est) null else est },
                        label = { Text("${est.etiqueta} (${lista.count { it.estado == est }})") }
                    )
                }
            }
            if (visibles.isEmpty()) {
                EstadoVacio(R.drawable.ic_inbox, "No hay viajes aquí", "Toca \"Agregar viaje\" para cargar uno.")
            } else {
                LazyColumn(contentPadding = FinDeLista, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(visibles, key = { it.id }) { v -> TarjetaViaje(v, ahora) { edicion = Edicion(v) } }
                }
            }
        }
    }
}

@Composable
private fun FormularioViaje(vm: OficinaViewModel, actual: ViajeDetalle?, onListo: () -> Unit) {
    val rutas by vm.rutas.collectAsStateWithLifecycle()
    val unidades by vm.unidades.collectAsStateWithLifecycle()

    var idRuta by rememberSaveable { mutableStateOf(actual?.idRuta) }
    var destino by rememberSaveable { mutableStateOf(actual?.destino.orEmpty()) }
    var idUnidad by rememberSaveable { mutableStateOf(actual?.idUnidad) }
    var salida by rememberSaveable { mutableStateOf(actual?.fechaSalida) }
    var llegada by rememberSaveable { mutableStateOf(actual?.fechaLlegada) }
    var entrega by rememberSaveable { mutableStateOf(actual?.fechaEntrega) }
    var error by remember { mutableStateOf<String?>(null) }

    val ruta = rutas.find { it.id == idRuta }
    val unidad = unidades.find { it.id == idUnidad }
    val programado = actual?.idProgramacion != null
    val valido = ruta != null && destino.isNotBlank() && (!programado || idUnidad != null)

    Formulario(error, if (actual == null) "Agregar viaje" else "Guardar cambios", valido, onGuardar = {
        if (ruta != null) {
            val v = Viaje(
                id = actual?.id ?: nuevoId(),
                idRuta = ruta.id,
                idUnidad = idUnidad,
                idOperador = actual?.idOperador,
                destino = destino.trim(),
                fechaSalida = salida,
                fechaLlegada = llegada,
                fechaEntrega = entrega
            )
            vm.guardarViaje(v) { err -> error = err; if (err == null) onListo() }
        }
    }) {
        if (rutas.isEmpty()) MensajeError("Primero carga una ruta en el módulo Rutas (número de despacho, recorrido y factura).")
        Selector(
            etiqueta = "Despacho y ruta",
            opciones = rutas,
            seleccion = ruta,
            texto = { it.texto() },
            ayuda = ruta?.let { "Factura ${it.numeroFactura}" }
        ) { idRuta = it.id; error = null }
        CampoTexto("Destino", destino, { destino = it; error = null })
        Selector<UnidadDetalle?>(
            etiqueta = "Unidad",
            opciones = (if (programado) emptyList() else listOf(null)) + unidades,
            seleccion = unidad,
            texto = { it?.texto() ?: "Sin asignar" },
            ayuda = when {
                idUnidad != null && unidad == null -> "La unidad que tenía este viaje ya fue deshecha."
                programado -> "El viaje está programado: necesita una unidad."
                else -> "Opcional. También se asigna al programar el viaje."
            }
        ) { idUnidad = it?.id; error = null }

        CampoFechaHora("Fecha de salida", salida) { salida = it; error = null }
        CampoFechaHora("Fecha de llegada", llegada) { llegada = it; error = null }
        CampoFechaHora("Fecha de entrega del producto", entrega) { entrega = it; error = null }
        val s = salida
        val l = llegada
        Dato(
            R.drawable.ic_edit_calendar, "Duración",
            if (s != null && l != null && l >= s) duracion(l - s) else "Se calcula con la salida y la llegada"
        )
    }
}

// ---------------- Programación: viajes en curso + programar ----------------

@Composable
fun ModuloProgramacion(vm: OficinaViewModel, snackbar: SnackbarHostState, onVolver: () -> Unit, avisar: (String) -> Unit) {
    val enCurso by vm.viajesEnCurso.collectAsStateWithLifecycle()
    var programando by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = programando) { programando = false }
    val ahora = rememberAhora(30_000)

    if (programando) {
        PantallaModulo("Programar viaje", snackbar, onVolver = { programando = false }) {
            FormularioProgramacion(vm) {
                programando = false
                avisar("Viaje programado. El operador ya lo ve en su teléfono.")
            }
        }
        return
    }

    PantallaModulo(Modulo.PROGRAMACION.titulo, snackbar, onVolver, "Programar", onAgregar = { programando = true }) {
        LazyColumn(contentPadding = FinDeLista, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val enRuta = enCurso.count { it.estado == EstadoViaje.EN_RUTA }
                Text(
                    "Viajes en curso: $enRuta en ruta, ${enCurso.size - enRuta} por salir",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (enCurso.isEmpty()) {
                item { EstadoVacio(R.drawable.ic_edit_calendar, "No hay viajes en curso", "Toca \"Programar\" para asignarle unidad y carga a un viaje.") }
            }
            items(enCurso, key = { it.id }) { TarjetaViaje(it, ahora) }
        }
    }
}

@Composable
private fun FormularioProgramacion(vm: OficinaViewModel, onListo: () -> Unit) {
    val viajes by vm.viajes.collectAsStateWithLifecycle()
    val unidades by vm.unidades.collectAsStateWithLifecycle()
    val combustibles by vm.combustibles.collectAsStateWithLifecycle()
    val sinProgramar = viajes.filter { it.estado == EstadoViaje.SIN_PROGRAMAR }

    var idViaje by rememberSaveable { mutableStateOf<String?>(null) }
    var idUnidad by rememberSaveable { mutableStateOf<String?>(null) }
    var idCarga by rememberSaveable { mutableStateOf<String?>(null) }
    var fechaProgramada by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember { mutableStateOf<String?>(null) }

    val viaje = sinProgramar.find { it.id == idViaje }
    val unidad = unidades.find { it.id == idUnidad }
    val carga = combustibles.find { it.id == idCarga }

    Formulario(
        error, "Programar viaje", viaje != null && unidad != null && carga != null,
        onGuardar = {
            if (viaje != null && unidad != null && carga != null) {
                vm.programarViaje(viaje.id, unidad.id, carga.id, fechaProgramada) { err -> error = err; if (err == null) onListo() }
            }
        },
        icono = R.drawable.ic_edit_calendar_fill,
        aviso = "Al programar, ${unidad?.nombreOperador ?: "el operador de la unidad"} verá el viaje en su teléfono."
    ) {
        if (sinProgramar.isEmpty()) MensajeError("No hay viajes sin programar. Agrega uno en el módulo Viajes.")
        if (unidades.isEmpty()) MensajeError("No hay unidades. Conforma una en el módulo Unidades.")

        Selector("Viaje", sinProgramar, viaje, { "Despacho ${it.numeroDespacho} a ${it.destino}" }) { v ->
            idViaje = v.id
            // Si el viaje ya traía unidad, viene elegida
            if (v.idUnidad != null && unidades.any { it.id == v.idUnidad }) idUnidad = v.idUnidad
            unidades.find { it.id == idUnidad }?.let { if (idCarga == null) idCarga = it.idTipoCombustible }
            error = null
        }
        Selector(
            etiqueta = "Unidad",
            opciones = unidades,
            seleccion = unidad,
            texto = { it.texto() + if (it.enUso) " (en ruta)" else "" },
            ayuda = unidad?.let { "Tanque de ${litros(it.volumenLitros)} para ${it.combustible}" }
        ) { u ->
            idUnidad = u.id
            idCarga = u.idTipoCombustible // la carga que lleva el tanque, se puede cambiar
            error = null
        }
        Selector(
            etiqueta = "Tipo de carga",
            opciones = combustibles,
            seleccion = carga,
            texto = { it.nombre },
            ayuda = if (unidad != null && carga != null && carga.id != unidad.idTipoCombustible)
                "Ojo: el tanque de esta unidad es para ${unidad.combustible}."
            else if (unidad != null) "La carga que lleva el tanque; se puede cambiar." else null
        ) { idCarga = it.id; error = null }
        CampoFecha("Fecha", fechaProgramada) { fechaProgramada = it }
    }
}
