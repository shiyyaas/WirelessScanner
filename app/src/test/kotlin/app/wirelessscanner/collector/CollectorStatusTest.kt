package app.wirelessscanner.collector

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CollectorStatusTest {

    @Test
    fun `collector status states represent distinct lifecycle phases`() {
        val idle: CollectorStatus = CollectorStatus.Idle
        val starting: CollectorStatus = CollectorStatus.Starting
        val observing: CollectorStatus = CollectorStatus.Observing
        val unavailable: CollectorStatus = CollectorStatus.Unavailable("Hardware Bluetooth disabled")
        val error: CollectorStatus = CollectorStatus.Error("Scan failed with code 2")

        assertThat(idle).isInstanceOf(CollectorStatus.Idle::class.java)
        assertThat(starting).isInstanceOf(CollectorStatus.Starting::class.java)
        assertThat(observing).isInstanceOf(CollectorStatus.Observing::class.java)

        assertThat(unavailable).isInstanceOf(CollectorStatus.Unavailable::class.java)
        assertThat((unavailable as CollectorStatus.Unavailable).reason)
            .isEqualTo("Hardware Bluetooth disabled")

        assertThat(error).isInstanceOf(CollectorStatus.Error::class.java)
        assertThat((error as CollectorStatus.Error).message)
            .isEqualTo("Scan failed with code 2")
    }
}
