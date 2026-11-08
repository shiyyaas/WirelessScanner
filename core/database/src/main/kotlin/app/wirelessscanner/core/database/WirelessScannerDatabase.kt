package app.wirelessscanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import app.wirelessscanner.core.database.converter.WirelessDatabaseConverters
import app.wirelessscanner.core.database.dao.ObservationDao
import app.wirelessscanner.core.database.dao.ScanSessionDao
import app.wirelessscanner.core.database.entity.ObservationEntity
import app.wirelessscanner.core.database.entity.ScanSessionEntity

@Database(
    entities = [ObservationEntity::class, ScanSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(WirelessDatabaseConverters::class)
abstract class WirelessScannerDatabase : RoomDatabase() {

    abstract fun observationDao(): ObservationDao
    abstract fun scanSessionDao(): ScanSessionDao

    companion object {
        private const val DATABASE_NAME = "wireless_scanner.db"

        @Volatile
        private var INSTANCE: WirelessScannerDatabase? = null

        fun getInstance(context: Context): WirelessScannerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WirelessScannerDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
