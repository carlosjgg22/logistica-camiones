package com.flota.despacho

import android.app.Application
import com.flota.despacho.data.FlotaRepository
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
        SyncWorker.programarPeriodico(this)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { repo.cargarDatosDemoSiVacio() }
    }
}
