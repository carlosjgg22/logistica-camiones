package com.flota.despacho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.flota.despacho.ui.login.LoginScreen
import com.flota.despacho.ui.oficina.OficinaScreen
import com.flota.despacho.ui.operador.OperadorScreen
import com.flota.despacho.ui.theme.FlotaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FlotaApp
        setContent { FlotaTheme { AppNav(app) } }
    }
}

@Composable
private fun AppNav(app: FlotaApp) {
    val nav = rememberNavController()
    val inicio = remember {
        when {
            app.sesion.idOperador != null -> "operador"
            app.sesion.esOficina -> "oficina"
            else -> "login"
        }
    }
    val cerrarSesion: () -> Unit = {
        app.sesion.cerrar()
        nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = inicio) {
        composable("login") {
            LoginScreen(
                app = app,
                onOperador = { id ->
                    app.sesion.idOperador = id
                    nav.navigate("operador") { popUpTo("login") { inclusive = true } }
                },
                onOficina = {
                    app.sesion.esOficina = true
                    nav.navigate("oficina") { popUpTo("login") { inclusive = true } }
                }
            )
        }
        composable("operador") {
            val id = app.sesion.idOperador
            if (id != null) OperadorScreen(app, id, onSalir = cerrarSesion)
        }
        composable("oficina") {
            OficinaScreen(app, onSalir = cerrarSesion)
        }
    }
}
