package com.flota.despacho.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TipoVehiculo::class, TipoCombustible::class, Operador::class, Vehiculo::class,
        Tanque::class, Unidad::class, Ruta::class, Viaje::class, Programacion::class,
        Inspeccion::class, Averia::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): FlotaDao

    companion object {
        @Volatile private var instancia: AppDatabase? = null

        /** 4 → 5: checklist y averías. Solo agrega tablas: no se pierde lo que falta enviar. */
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Copiado de lo que genera Room (AppDatabase_Impl): tiene que coincidir exacto
                db.execSQL("CREATE TABLE IF NOT EXISTS `inspeccion` (`id` TEXT NOT NULL, `idViaje` TEXT NOT NULL, `idUnidad` TEXT NOT NULL, `idOperador` TEXT NOT NULL, `momento` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `kilometraje` INTEGER NOT NULL, `resultados` TEXT NOT NULL, `observaciones` TEXT NOT NULL, `pendienteSync` INTEGER NOT NULL, `actualizado` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspeccion_idViaje` ON `inspeccion` (`idViaje`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_inspeccion_idOperador` ON `inspeccion` (`idOperador`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `averia` (`id` TEXT NOT NULL, `idInspeccion` TEXT NOT NULL, `idViaje` TEXT NOT NULL, `idUnidad` TEXT NOT NULL, `idOperador` TEXT NOT NULL, `idVehiculo` TEXT, `idTanque` TEXT, `codigo` TEXT NOT NULL, `punto` TEXT NOT NULL, `gravedad` TEXT NOT NULL, `observacion` TEXT NOT NULL, `momento` TEXT NOT NULL, `fecha` INTEGER NOT NULL, `resuelta` INTEGER NOT NULL, `fechaResuelta` INTEGER, `resueltaPor` TEXT, `pendienteSync` INTEGER NOT NULL, `actualizado` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_averia_idVehiculo` ON `averia` (`idVehiculo`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_averia_idTanque` ON `averia` (`idTanque`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_averia_idViaje` ON `averia` (`idViaje`)")
            }
        }

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "flota.db"
                )
                    .addMigrations(MIGRACION_4_5)
                    // Versiones más viejas que la 4 (solo de desarrollo): se recrea la base
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instancia = it }
            }
    }
}
