package com.flota.despacho.ui.componentes

import com.flota.despacho.R
import androidx.annotation.DrawableRes
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.flota.despacho.data.db.EstadoViaje
import com.flota.despacho.ui.theme.colorEstado
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val es = Locale.forLanguageTag("es")

fun fechaHora(ms: Long): String = SimpleDateFormat("dd/MM/yyyy, HH:mm", es).format(Date(ms))
fun hora(ms: Long): String = SimpleDateFormat("HH:mm", es).format(Date(ms))
fun fecha(ms: Long): String =
    SimpleDateFormat("EEEE d 'de' MMMM", es).format(Date(ms)).replaceFirstChar { it.uppercase() }
fun litros(l: Int): String = NumberFormat.getIntegerInstance(es).format(l) + " L"

fun duracion(ms: Long): String {
    val minutos = (ms / 60_000).coerceAtLeast(0)
    val h = minutos / 60
    val m = minutos % 60
    return if (h > 0) "$h h $m min" else "$m min"
}

/** Hoy a la hora y minuto indicados. */
fun horaDeHoy(h: Int, m: Int): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Hora actual que se refresca sola, para mostrar "Ahora, 14:32" o tiempos en ruta. */
@Composable
fun rememberAhora(cadaMs: Long = 15_000): Long {
    var ahora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(cadaMs) {
        while (true) { delay(cadaMs); ahora = System.currentTimeMillis() }
    }
    return ahora
}

/** Botón alto y ancho: fácil de tocar dentro de la cabina. */
@Composable
fun BotonGrande(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icono: Int? = null,
    enabled: Boolean = true,
    secundario: Boolean = false
) {
    val colores = if (secundario) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        )
    } else ButtonDefaults.buttonColors()

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 68.dp),
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        colors = colores
    ) {
        if (icono != null) {
            Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(texto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChipTexto(texto: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = RoundedCornerShape(50)) {
        Text(
            texto,
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ChipEstado(estado: EstadoViaje) = ChipTexto(estado.etiqueta, colorEstado(estado))

@Composable
fun AvisoSync(pendientes: Int, modifier: Modifier = Modifier) {
    val enviado = pendientes == 0
    Surface(
        color = if (enviado) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(if (enviado) R.drawable.ic_cloud_done_fill else R.drawable.ic_cloud_upload_fill), contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(
                if (enviado) "Todo enviado"
                else "Guardado en el teléfono ($pendientes por enviar). Se envía solo cuando haya señal.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Aviso informativo (no es error): fondo surfaceVariant con ícono info. */
@Composable
fun AvisoInfo(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MensajeError(texto: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_error_fill), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Text(texto, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun Dato(@DrawableRes icono: Int, etiqueta: String, valor: String, destacado: Boolean = false) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painterResource(icono), contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 2.dp).size(24.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                valor,
                style = if (destacado) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                fontWeight = if (destacado) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun Secundario(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun EstadoVacio(@DrawableRes icono: Int, titulo: String, texto: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(painterResource(icono), contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
        Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(texto, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/**
 * Campo de hora grande. null = "ahora" (se toma la hora real al confirmar).
 * Al tocarlo se abre un reloj para corregir la hora si el operador se olvidó de registrarla a tiempo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoHora(etiqueta: String, elegida: Long?, onElegir: (Long?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val ahora = rememberAhora()

    OutlinedCard(
        onClick = { abierto = true },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_schedule), contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (elegida == null) "Ahora, ${hora(ahora)}" else "Hoy a las ${hora(elegida)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Secundario("Toca para cambiar la hora")
            }
            Icon(painterResource(R.drawable.ic_edit), contentDescription = "Cambiar hora")
        }
    }

    if (abierto) {
        val base = Calendar.getInstance().apply { timeInMillis = elegida ?: System.currentTimeMillis() }
        val estado = rememberTimePickerState(
            initialHour = base.get(Calendar.HOUR_OF_DAY),
            initialMinute = base.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { abierto = false },
            title = { Text(etiqueta) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = { onElegir(horaDeHoy(estado.hour, estado.minute)); abierto = false }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { onElegir(null); abierto = false }) { Text("Usar hora actual") }
                    TextButton(onClick = { abierto = false }) { Text("Cancelar") }
                }
            }
        )
    }
}

/** DatePicker trabaja en UTC; esto evita que la fecha se corra un día. */
private fun aUtcMedianoche(local: Long): Long =
    Instant.ofEpochMilli(local).atZone(ZoneId.systemDefault()).toLocalDate()
        .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** Fecha elegida en el DatePicker (UTC) + hora y minuto locales. */
private fun deUtcALocal(utc: Long, h: Int, m: Int): Long =
    Instant.ofEpochMilli(utc).atZone(ZoneOffset.UTC).toLocalDate()
        .atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

/**
 * Fecha y hora opcionales (salida, llegada, entrega). Primero se elige el día y después la hora.
 * null = todavía no pasó; se puede borrar con "Quitar".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFechaHora(etiqueta: String, valor: Long?, onCambio: (Long?) -> Unit) {
    var paso by remember { mutableIntStateOf(0) } // 0 cerrado, 1 día, 2 hora
    var dia by remember { mutableLongStateOf(0L) }

    OutlinedCard(
        onClick = { paso = 1 },
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_event), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(valor?.let(::fechaHora) ?: "Sin cargar", style = MaterialTheme.typography.bodyLarge)
            }
            if (valor != null) TextButton(onClick = { onCambio(null) }) { Text("Quitar") }
        }
    }

    if (paso == 1) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = aUtcMedianoche(valor ?: System.currentTimeMillis()))
        DatePickerDialog(
            onDismissRequest = { paso = 0 },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { dia = it; paso = 2 } ?: run { paso = 0 }
                }) { Text("Siguiente") }
            },
            dismissButton = { TextButton(onClick = { paso = 0 }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
    if (paso == 2) {
        val base = Calendar.getInstance().apply { timeInMillis = valor ?: System.currentTimeMillis() }
        val estado = rememberTimePickerState(
            initialHour = base.get(Calendar.HOUR_OF_DAY),
            initialMinute = base.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { paso = 0 },
            title = { Text(etiqueta) },
            text = { TimePicker(state = estado) },
            confirmButton = {
                TextButton(onClick = { onCambio(deUtcALocal(dia, estado.hour, estado.minute)); paso = 0 }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { paso = 0 }) { Text("Cancelar") } }
        )
    }
}

/** Solo el día (fecha programada). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoFecha(etiqueta: String, valor: Long, onCambio: (Long) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { abierto = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
        Icon(painterResource(R.drawable.ic_calendar_today), contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text("$etiqueta: ${fecha(valor)}")
    }
    if (abierto) {
        val estado = rememberDatePickerState(initialSelectedDateMillis = aUtcMedianoche(valor))
        DatePickerDialog(
            onDismissRequest = { abierto = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { onCambio(deUtcALocal(it, 6, 0)) }
                    abierto = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { abierto = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estado) }
    }
}

/** Campo de texto de formulario con el estilo de la app. */
@Composable
fun CampoTexto(
    etiqueta: String,
    valor: String,
    onCambio: (String) -> Unit,
    numerico: Boolean = false,
    sufijo: String? = null,
    error: String? = null
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onCambio(if (numerico) it.filter(Char::isDigit) else it) },
        label = { Text(etiqueta) },
        suffix = sufijo?.let { { Text(it) } },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numerico) KeyboardType.Number else KeyboardType.Text,
            capitalization = if (numerico) KeyboardCapitalization.None else KeyboardCapitalization.Sentences
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Confirmacion(
    titulo: String,
    texto: String,
    boton: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { Button(onClick = onConfirmar) { Text(boton) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** Lista desplegable reutilizable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Selector(
    etiqueta: String,
    opciones: List<T>,
    seleccion: T?,
    texto: (T) -> String,
    ayuda: String? = null,
    onSeleccion: (T) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = seleccion?.let(texto) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            supportingText = if (ayuda != null) {
                { Text(ayuda) }
            } else null,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(texto(opcion), style = MaterialTheme.typography.bodyLarge) },
                    onClick = { onSeleccion(opcion); abierto = false },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
