package info.nightscout.androidaps.plugins.pump.danaR.comm

import info.nightscout.androidaps.interfaces.PumpDescription
import info.nightscout.androidaps.plugins.pump.danaR.DanaRPlugin
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(DanaRPlugin::class)
class MsgSettingBasalTest : DanaRTestBase() {

    @Mock lateinit var danaRPlugin: DanaRPlugin

    @Test fun runTest() {
        `when`(danaRPlugin.getPumpDescription()).thenReturn(PumpDescription())
        val packet = MsgSettingBasal(aapsLogger, danaRPump, danaRPlugin)

        // test message decoding
        packet.handleMessage(createArray(100, 1.toByte()))
        val expected = MessageBase.intFromBuff(createArray(100, 1.toByte()), 2 * 1, 2)
        Assert.assertEquals(expected.toDouble() / 100.0, danaRPump.pumpProfiles!![danaRPump.activeProfile][1], 0.0)
    }
}