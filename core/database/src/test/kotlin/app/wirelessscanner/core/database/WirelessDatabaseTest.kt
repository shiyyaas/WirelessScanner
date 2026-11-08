package app.wirelessscanner.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.wirelessscanner.core.database.dao.ObservationDao
import app.wirelessscanner.core.database.dao.ScanSessionDao
import app.wirelessscanner.core.database.entity.ObservationEntity
import app.wirelessscanner.core.database.entity.ScanSessionEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WirelessDatabaseTest {

    private lateinit var database: WirelessScannerDatabase
    private lateinit var observationDao: ObservationDao
    private lateinit var scanSessionDao: ScanSessionDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WirelessScannerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        observationDao = database.observationDao()
        scanSessionDao = database.scanSessionDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndRetrieveObservation() = runBlocking {
        val session = ScanSessionEntity(
            sessionId = "session_1",
            startTimeEpochMillis = 1000L,
            name = "Test Session",
            isOngoing = true
        )
        scanSessionDao.insertSession(session)

        val entity = ObservationEntity(
            id = "obs_1",
            timestampEpochMillis = 1500L,
            radioType = "BLE",
            receiverIdentifier = "internal_host",
            receiverDisplayName = "Internal Device Radio",
            rawAddress = "AA:BB:CC:DD:EE:FF",
            advertisedName = "BeaconX",
            rssi = -65,
            rawPayloadHex = "020106",
            sessionId = "session_1"
        )
        observationDao.insertObservation(entity)

        val observations = observationDao.getAllObservationsFlow().first()
        assertThat(observations).hasSize(1)
        assertThat(observations[0].rawAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(observations[0].rssi).isEqualTo(-65)
        assertThat(observations[0].advertisedName).isEqualTo("BeaconX")
    }

    @Test
    fun searchObservationsByQuery() = runBlocking {
        val entity1 = ObservationEntity(
            id = "obs_1",
            timestampEpochMillis = 1500L,
            radioType = "BLE",
            receiverIdentifier = "internal_host",
            receiverDisplayName = "Internal Device Radio",
            rawAddress = "AA:BB:CC:DD:EE:FF",
            advertisedName = "AlphaDevice",
            rssi = -65,
            rawPayloadHex = ""
        )
        val entity2 = ObservationEntity(
            id = "obs_2",
            timestampEpochMillis = 1600L,
            radioType = "WIFI",
            receiverIdentifier = "internal_host",
            receiverDisplayName = "Internal Device Radio",
            rawAddress = "11:22:33:44:55:66",
            advertisedName = "BetaRouter",
            rssi = -50,
            rawPayloadHex = ""
        )
        observationDao.insertObservations(listOf(entity1, entity2))

        val results = observationDao.searchObservations("%Alpha%").first()
        assertThat(results).hasSize(1)
        assertThat(results[0].id).isEqualTo("obs_1")
    }
}
