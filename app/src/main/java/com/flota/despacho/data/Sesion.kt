package com.flota.despacho.data

import android.content.Context

/** Recuerda quién entró, para que el operador no tenga que loguearse cada vez. */
class Sesion(context: Context) {
    private val prefs = context.getSharedPreferences("sesion", Context.MODE_PRIVATE)

    var idOperador: String?
        get() = prefs.getString("idOperador", null)
        set(valor) = prefs.edit().putString("idOperador", valor).apply()

    var esOficina: Boolean
        get() = prefs.getBoolean("esOficina", false)
        set(valor) = prefs.edit().putBoolean("esOficina", valor).apply()

    fun cerrar() = prefs.edit().clear().apply()
}
