package com.flota.despacho.sync

import android.content.Context
import androidx.work.*
import com.flota.despacho.FlotaApp
import java.util.concurrent.TimeUnit

/** Sube y baja datos. WorkManager reintenta solo cuando vuelve la señal, aunque la app esté cerrada. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result =
        try {
            (applicationContext as FlotaApp).repo.sincronizar()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }

    companion object {
        private val conRed = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        fun programar(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(conRed)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("sync", ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
        }

        fun programarPeriodico(context: Context) {
            val trabajo = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(conRed)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("sync-periodico", ExistingPeriodicWorkPolicy.KEEP, trabajo)
        }
    }
}
