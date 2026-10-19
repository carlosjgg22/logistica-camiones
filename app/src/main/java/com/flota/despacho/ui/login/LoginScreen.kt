package com.flota.despacho.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flota.despacho.FlotaApp
import com.flota.despacho.ui.componentes.BotonGrande
import kotlinx.coroutines.launch

// Solo para la demo. Reemplázalo por login real (JWT) contra el backend.
private const val PIN_OFICINA = "1234"

@Composable
fun LoginScreen(app: FlotaApp, onOperador: (String) -> Unit, onOficina: () -> Unit) {
    var modo by rememberSaveable { mutableStateOf<String?>(null) } // "operador" | "oficina"
    var texto by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun entrar() {
        if (texto.isBlank() || cargando) return
        scope.launch {
            cargando = true
            if (modo == "operador") {
                val op = app.repo.operadorPorCedula(texto)
                if (op != null) onOperador(op.id)
                else error = "No hay ningún operador con esa cédula. Revisa el número o llama a la oficina."
            } else {
                if (texto == PIN_OFICINA) onOficina() else error = "PIN incorrecto."
            }
            cargando = false
        }
    }

    Scaffold { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.LocalShipping, contentDescription = null,
                modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary
            )
            Text("Despachos", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

            when (modo) {
                null -> {
                    Text("¿Cómo vas a usar la app?", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    BotonGrande("Soy operador", onClick = { modo = "operador" }, icono = Icons.Filled.Person)
                    OutlinedButton(
                        onClick = { modo = "oficina" },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                    ) {
                        Icon(Icons.Filled.Business, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Ver conductores y viajes", style = MaterialTheme.typography.titleMedium)
                    }
                }
                else -> {
                    val esOperador = modo == "operador"
                    val mensajeError = error
                    Text(
                        if (esOperador) "Escribe tu número de cédula" else "Escribe el PIN de acceso",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    OutlinedTextField(
                        value = texto,
                        onValueChange = { nuevo -> texto = nuevo.filter(Char::isDigit); error = null },
                        label = { Text(if (esOperador) "Cédula" else "PIN") },
                        singleLine = true,
                        isError = error != null,
                        supportingText = if (mensajeError != null) {
                            { Text(mensajeError) }
                        } else null,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(letterSpacing = 2.sp),
                        visualTransformation = if (esOperador) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (esOperador) KeyboardType.Number else KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { entrar() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    BotonGrande("Entrar", onClick = { entrar() }, enabled = texto.isNotBlank() && !cargando)
                    TextButton(onClick = { modo = null; texto = ""; error = null }) { Text("Volver") }
                    Text(
                        "Prueba: cédulas 12345678 (Juan), 87654321 (María) o 11223344 (Pedro). PIN 1234",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
