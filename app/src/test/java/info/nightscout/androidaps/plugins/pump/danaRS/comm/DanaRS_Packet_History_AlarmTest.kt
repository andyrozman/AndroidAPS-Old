package info.nightscout.androidaps.plugins.pump.danaRS.comm

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest()
class DanaRS_Packet_History_AlarmTest : DanaRSTestBase() {

    @Test fun runTest() {
        val packet = DanaRS_Packet_History_Alarm(aapsLogger, rxBus, System.currentTimeMillis())
        Assert.assertEquals("REVIEW__ALARM", packet.friendlyName)
    }
}