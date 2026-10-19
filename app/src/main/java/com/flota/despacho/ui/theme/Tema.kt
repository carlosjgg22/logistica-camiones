package com.flota.despacho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.flota.despacho.data.db.EstadoViaje

// Azul de flota + ámbar de combustible. Alto contraste para leer bajo el sol.
private val Claro = lightColorScheme(
    primary = Color(0xFF0B4F8A), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E4FF), onPrimaryContainer = Color(0xFF001C38),
    secondary = Color(0xFFC25A00), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDCC2), onSecondaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF4F6F9), onBackground = Color(0xFF15181C),
    surface = Color(0xFFF4F6F9), onSurface = Color(0xFF15181C),
    surfaceVariant = Color(0xFFE2E7EE), onSurfaceVariant = Color(0xFF424850),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFEDF0F4), surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFA2C9FF), onPrimary = Color(0xFF00315B),
    primaryContainer = Color(0xFF004880), onPrimaryContainer = Color(0xFFD3E4FF),
    secondary = Color(0xFFFFB77C), onSecondary = Color(0xFF4C2700),
    secondaryContainer = Color(0xFF6C3A00), onSecondaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF101318), onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF101318), onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF2B3038), onSurfaceVariant = Color(0xFFC3C7CF),
    surfaceContainerLowest = Color(0xFF0B0E12), surfaceContainerLow = Color(0xFF1A1E24),
    surfaceContainer = Color(0xFF1A1E24), surfaceContainerHigh = Color(0xFF22262D),
    surfaceContainerHighest = Color(0xFF22262D),
)

private val base = Typography()
private val Tipografia = base.copy(
    bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 22.sp),
    titleMedium = base.titleMedium.copy(fontSize = 18.sp),
    labelLarge = base.labelLarge.copy(fontSize = 16.sp),
)

@Composable
fun FlotaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Oscuro else Claro,
        typography = Tipografia,
        content = content
    )
}

@Composable
fun colorEstado(estado: EstadoViaje): Color {
    val oscuro = isSystemInDarkTheme()
    return when (estado) {
        EstadoViaje.PROGRAMADO -> if (oscuro) Color(0xFFA9B4FF) else Color(0xFF3949AB)
        EstadoViaje.EN_RUTA -> colorOcupado()
        EstadoViaje.COMPLETADO -> colorDisponible()
        EstadoViaje.SIN_PROGRAMAR -> if (oscuro) Color(0xFFB0B4BA) else Color(0xFF616870)
    }
}

@Composable
fun colorDisponible(): Color = if (isSystemInDarkTheme()) Color(0xFF8BD88F) else Color(0xFF2E7D32)

@Composable
fun colorOcupado(): Color = if (isSystemInDarkTheme()) Color(0xFFFFB77C) else Color(0xFFB35300)
