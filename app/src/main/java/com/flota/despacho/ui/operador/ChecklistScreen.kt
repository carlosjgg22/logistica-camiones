package com.flota.despacho.ui.operador

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.flota.despacho.R
import com.flota.despacho.data.*
import com.flota.despacho.data.db.ViajeDetalle
import com.flota.despacho.ui.componentes.*
import com.flota.despacho.ui.theme.colorDisponible
import com.flota.despacho.ui.theme.colorOcupado

/**
 * Inspección CHK-CTR-01 antes de salir o al llegar. Los datos del encabezado (conductor, placas,
 * producto, fecha) salen del viaje; el operador escribe el kilometraje y marca B / M / N/A por punto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(
    vm: OperadorViewModel,
    viaje: ViajeDetalle,
    momento: Momento,
    onVolver: () -> Unit,
    onGuardado: (averias: Int) -> Unit
) {
    BackHandler(onBack = onVolver)
    var confirmar by remember { mutableStateOf(false) }
    val ahora = rememberAhora()

    val respondidos = PUNTOS_CHECKLIST.count { vm.respuestas.containsKey(it.codigo) }
    val malos = vm.respuestas.values.filter { it.resultado == Resultado.M.name }
    val graves = malos.count { it.gravedad == Gravedad.GRAVE.name }
    val incompletos = malos.count { it.gravedad == null || it.observacion.isBlank() }
    val completo = respondidos == PUNTOS_CHECKLIST.size && incompletos == 0 && vm.kilometraje.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checklist de ${momento.etiqueta}") },
                navigationIcon = {
                    IconButton(onClick = onVolver) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = "Volver") }
                }
            )
        },
        bottomBar = {
            Column(
                Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Secundario(
                    when {
                        respondidos < PUNTOS_CHECKLIST.size -> "Faltan ${PUNTOS_CHECKLIST.size - respondidos} de ${PUNTOS_CHECKLIST.size} puntos"
                        incompletos > 0 -> "Completa leve o grave y qué pasa en los puntos en M"
                        vm.kilometraje.isBlank() -> "Falta el kilometraje"
                        malos.isEmpty() -> "Todo revisado, sin novedades"
                        else -> "Todo revisado: ${averiasTexto(malos.size, graves)}"
                    }
                )
                if (graves > 0 && momento == Momento.SALIDA) {
                    MensajeError("Con una avería grave no vas a poder salir hasta que la oficina la resuelva.")
                }
                vm.errorChecklist?.let { MensajeError(it) }
                BotonGrande(
                    "Guardar checklist",
                    onClick = { if (malos.isEmpty()) vm.guardarChecklist(viaje.id, momento, onGuardado) else confirmar = true },
                    icono = R.drawable.ic_fact_check_fill,
                    enabled = completo
                )
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Encabezado(vm, viaje, momento, ahora) }
            Seccion.entries.forEach { seccion ->
                val puntos = PUNTOS_CHECKLIST.filter { it.seccion == seccion }
                item(key = seccion.name) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(seccion.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        // Lo más común es que todo esté bien: un toque marca B en lo que falte de la sección
                        TextButton(onClick = {
                            puntos.filter { !vm.respuestas.containsKey(it.codigo) }
                                .forEach { vm.responder(ResultadoPunto(it.codigo, Resultado.B.name)) }
                        }) { Text("Todo bien") }
                    }
                }
                items(puntos, key = { it.codigo }) { p -> TarjetaPunto(p, vm.respuestas[p.codigo], vm::responder) }
            }
            item {
                OutlinedTextField(
                    value = vm.observaciones,
                    onValueChange = { vm.observaciones = it },
                    label = { Text("Observaciones generales / novedades (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (confirmar) {
        Confirmacion(
            titulo = "¿Reportar ${averiasTexto(malos.size, graves)}?",
            texto = "La oficina las verá en el panel. " +
                if (graves > 0 && momento == Momento.SALIDA) "Con una grave no podrás salir hasta que la resuelvan." else "",
            boton = "Guardar y reportar",
            onConfirmar = { confirmar = false; vm.guardarChecklist(viaje.id, momento, onGuardado) },
            onCancelar = { confirmar = false }
        )
    }
}

fun averiasTexto(total: Int, graves: Int): String {
    val base = if (total == 1) "1 avería" else "$total averías"
    return if (graves > 0) "$base ($graves grave${if (graves > 1) "s" else ""})" else base
}

@Composable
private fun Encabezado(vm: OperadorViewModel, v: ViajeDetalle, momento: Momento, ahora: Long) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Inspección diaria de pre-uso · CHK-CTR-01", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Dato(R.drawable.ic_person, "Conductor", vm.nombre.ifBlank { v.operador ?: "-" })
            Dato(R.drawable.ic_local_shipping, "Placa chuto / cisterna", "${v.vehiculo ?: "-"} / ${v.tanque ?: "-"}")
            v.carga?.let { Dato(R.drawable.ic_local_gas_station, "Producto", it) }
            Dato(R.drawable.ic_schedule, "Fecha y hora", fechaHora(ahora))
            CampoTexto(
                if (momento == Momento.SALIDA) "Kilometraje inicial" else "Kilometraje final",
                vm.kilometraje, { vm.kilometraje = it }, numerico = true, sufijo = "km"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TarjetaPunto(p: PuntoChecklist, r: ResultadoPunto?, onCambio: (ResultadoPunto) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${p.codigo}  ${p.titulo}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Secundario(p.detalle)
            // B / M / N/A: botones anchos, fáciles de tocar con guantes
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Resultado.entries.forEach { opcion ->
                    val elegido = r?.resultado == opcion.name
                    val color = when (opcion) {
                        Resultado.B -> colorDisponible()
                        Resultado.M -> MaterialTheme.colorScheme.error
                        Resultado.NA -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    OutlinedButton(
                        onClick = { onCambio(ResultadoPunto(p.codigo, opcion.name, r?.gravedad.takeIf { opcion == Resultado.M }, r?.observacion.orEmpty())) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (elegido) color.copy(alpha = 0.14f) else Color.Transparent,
                            contentColor = if (elegido) color else MaterialTheme.colorScheme.onSurface
                        ),
                        border = if (elegido) BorderStroke(2.dp, color) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) { Text(opcion.corto, fontWeight = if (elegido) FontWeight.Bold else FontWeight.Normal) }
                }
            }
            if (r?.resultado == Resultado.M.name) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Gravedad.entries.forEachIndexed { i, g ->
                        val color = if (g == Gravedad.GRAVE) MaterialTheme.colorScheme.error else colorOcupado()
                        SegmentedButton(
                            selected = r.gravedad == g.name,
                            onClick = { onCambio(r.copy(gravedad = g.name)) },
                            shape = SegmentedButtonDefaults.itemShape(i, Gravedad.entries.size),
                            colors = SegmentedButtonDefaults.colors(activeContainerColor = color.copy(alpha = 0.14f), activeContentColor = color),
                            icon = {}
                        ) { Text(if (g == Gravedad.GRAVE) "Grave: no sale" else "Leve: puede salir") }
                    }
                }
                OutlinedTextField(
                    value = r.observacion,
                    onValueChange = { onCambio(r.copy(observacion = it)) },
                    label = { Text("¿Qué pasa?") },
                    isError = r.observacion.isBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
