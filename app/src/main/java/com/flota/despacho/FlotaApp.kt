package com.flota.despacho

import android.app.Application
import com.flota.despacho.data.FlotaRepository
import com.flota.despacho.data.Rol
import com.flota.despacho.data.Sesion
import com.flota.despacho.data.db.AppDatabase
import com.flota.despacho.sync.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlotaApp : Application() {
    lateinit var repo: FlotaRepository
        private set
    lateinit var sesion: Sesion
        private set

    override fun onCreate() {
        super.onCreate()
        repo = FlotaRepository(AppDatabase.get(this), this)
        sesion = Sesion(this)
        // "Mantener la sesión iniciada" sin marcar: el administrador vuelve a escribir su clave al abrir la app
        if (sesion.rol == Rol.ADMINISTRADOR && !sesion.mantener) {
            repo.cerrarSesionAdministrador()
            sesion.cerrar()
        }
        SyncWorker.programarPeriodico(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repo.cargarDatosDemoSiVacio() }
    }
}
