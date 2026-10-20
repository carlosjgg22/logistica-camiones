package com.flota.despacho.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flota.despacho.FlotaApp
import com.flota.despacho.R
import com.flota.despacho.data.Rol
import com.flota.despacho.ui.componentes.BotonGrande
import com.flota.despacho.ui.componentes.MensajeError
import kotlinx.coroutines.launch

/**
 * El operador entra con su cédula (y la sincronización usa la sesión anónima de Firebase).
 * El administrador entra con correo y contraseña de Firebase Auth: el mismo usuario del panel de la PC.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(app: FlotaApp, onOperador: (String) -> Unit, onAdministrador: (mantener: Boolean) -> Unit) {
    var rol by rememberSaveable { mutableStateOf(Rol.OPERADOR) }
    var cedula by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var clave by remember { mutableStateOf("") } // la contraseña no se guarda al rotar
    var verClave by remember { mutableStateOf(false) }
    var mantener by rememberSaveable { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var cargando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val esOperador = rol == Rol.OPERADOR

    fun entrar() {
        if (cargando) return
        if (esOperador && cedula.isBlank()) { error = "Escribe tu cédula."; return }
        if (!esOperador && (correo.isBlank() || clave.isEmpty())) { error = "Escribe tu correo y tu contraseña."; return }
        scope.launch {
            cargando = true
            if (esOperador) {
                val op = app.repo.operadorPorCedula(cedula)
                if (op != null) onOperador(op.id) else error = "No hay un operador con esa cédula."
            } else {
                error = app.repo.entrarAdministrador(correo, clave)
                if (error == null) onAdministrador(mantener)
            }
            cargando = false
        }
    }

    Scaffold(
        bottomBar = {
            if (!esOperador) {
                Text(
                    "El mismo usuario sirve para el panel de la PC.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)
                )
            }
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(if (esOperador) R.drawable.ic_local_shipping_fill else R.drawable.ic_apartment_fill),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Column {
                Text(
                    "LogisticaGarcía · Despachos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    if (esOperador) "Entra con tu cédula" else "Entrar a la oficina",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            SelectorRol(rol) { rol = it; error = null }

            if (esOperador) {
                Campo("Cédula") {
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = { cedula = it.filter(Char::isDigit); error = null },
                        singleLine = true,
                        isError = error != null,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 22.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { entrar() }),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)
                    )
                }
            } else {
                Campo("Correo") {
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it.trim(); error = null },
                        singleLine = true,
                        isError = error != null,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Campo("Contraseña") {
                    OutlinedTextField(
                        value = clave,
                        onValueChange = { clave = it; error = null },
                        singleLine = true,
                        isError = error != null,
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { verClave = !verClave }) {
                                Icon(
                                    painterResource(if (verClave) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                    contentDescription = if (verClave) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { entrar() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = mantener, role = Role.Checkbox, onValueChange = { mantener = it }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = mantener, onCheckedChange = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Mantener la sesión iniciada", style = MaterialTheme.typography.bodyMedium)
                }
            }

            error?.let { MensajeError(it) }

            BotonGrande(
                if (cargando) "Entrando…" else "Entrar",
                onClick = { entrar() },
                icono = R.drawable.ic_login_fill,
                enabled = !cargando
            )
        }
    }
}

/** Operador | Administrador, con el elegido en primaryContainer y una tilde. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorRol(rol: Rol, onCambio: (Rol) -> Unit) {
    val opciones = listOf(Rol.OPERADOR to "Operador", Rol.ADMINISTRADOR to "Administrador")
    val colores = SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        activeBorderColor = MaterialTheme.colorScheme.outline,
        inactiveContainerColor = Color.Transparent,
        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
        inactiveBorderColor = MaterialTheme.colorScheme.outline
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { i, (valor, texto) ->
            val elegido = rol == valor
            SegmentedButton(
                selected = elegido,
                onClick = { onCambio(valor) },
                shape = SegmentedButtonDefaults.itemShape(index = i, count = opciones.size),
                colors = colores,
                icon = {
                    SegmentedButtonDefaults.Icon(active = elegido, activeContent = {
                        Icon(
                            painterResource(R.drawable.ic_check), contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    })
                },
                modifier = Modifier.heightIn(min = 40.dp)
            ) { Text(texto, fontWeight = if (elegido) FontWeight.SemiBold else FontWeight.Normal) }
        }
    }
}

/** Etiqueta arriba del campo, como en el diseño. */
@Composable
private fun Campo(etiqueta: String, campo: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        campo()
    }
}
