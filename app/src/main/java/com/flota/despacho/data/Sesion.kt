package com.flota.despacho.data

import android.content.Context

enum class Rol { OPERADOR, ADMINISTRADOR }

/** Recuerda quién entró, para que el operador no tenga que loguearse cada vez. */
class Sesion(context: Context) {
    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    // Versión anterior: el operador solo guardaba su id; la oficina con PIN ya no vale y vuelve al login.
    var rol: Rol?
        get() = prefs.getString("rol", null)?.let { runCatching { Rol.valueOf(it) }.getOrNull() }
            ?: Rol.OPERADOR.takeIf { idOperador != null }
        set(valor) = prefs.edit().putString("rol", valor?.name).apply()

    /** Solo para el operador. */
    var idOperador: String?
        get() = prefs.getString("idOperador", null)
        set(valor) = prefs.edit().putString("idOperador", valor).apply()

    /** Si es false, la sesión del administrador se cierra la próxima vez que se abre la app. */
    var mantener: Boolean
        get() = prefs.getBoolean("mantener", true)
        set(valor) = prefs.edit().putBoolean("mantener", valor).apply()

    fun entrarOperador(id: String) =
        prefs.edit().clear().putString("rol", Rol.OPERADOR.name).putString("idOperador", id).apply()

    fun entrarAdministrador(mantener: Boolean) =
        prefs.edit().clear().putString("rol", Rol.ADMINISTRADOR.name).putBoolean("mantener", mantener).apply()

    fun cerrar() = prefs.edit().clear().apply()
}
