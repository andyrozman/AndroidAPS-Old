package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import info.nightscout.androidaps.plugins.pump.danaR.comm.DanaRTestBase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
class MsgStatusTempBasal_v2Test : DanaRTestBase() {

    @Test fun runTest() {
        val packet = MsgStatusTempBasal_v2(aapsLogger, danaRPump)
        // test message decoding
        packet.handleMessage(createArray(34, 1.toByte()))
        Assert.assertEquals(true, danaRPump.isTempBasalInProgress)
        // passing an bigger number
        packet.handleMessage(createArray(34, 2.toByte()))
        Assert.assertEquals(false, danaRPump.isTempBasalInProgress)
    }
}