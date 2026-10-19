package com.flota.despacho.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TipoVehiculo::class, TipoCombustible::class, Operador::class, Vehiculo::class,
        Tanque::class, Unidad::class, Ruta::class, Viaje::class, Programacion::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FlotaDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "flota.db"
                )
                    // En desarrollo: si cambia el esquema se recrea. En producción usa migraciones.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
