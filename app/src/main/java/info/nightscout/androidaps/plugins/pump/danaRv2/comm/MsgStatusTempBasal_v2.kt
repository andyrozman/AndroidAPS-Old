package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.danaR.DanaRPump
import info.nightscout.androidaps.plugins.pump.danaR.comm.MessageBase
import info.nightscout.androidaps.utils.DateUtil
import kotlin.math.ceil

class MsgStatusTempBasal_v2(
    private val aapsLogger: AAPSLogger,
    private val danaRPump: DanaRPump

) : MessageBase() {

    init {
        SetCommand(0x0205)
        aapsLogger.debug(LTag.PUMPCOMM, "New message")
    }

    override fun handleMessage(bytes: ByteArray) {
        val isTempBasalInProgress = intFromBuff(bytes, 0, 1) and 0x01 == 0x01
        val isAPSTempBasalInProgress = intFromBuff(bytes, 0, 1) and 0x02 == 0x02
        var tempBasalPercent = intFromBuff(bytes, 1, 1)
        if (tempBasalPercent > 200) tempBasalPercent = (tempBasalPercent - 200) * 10
        val tempBasalTotalSec: Int = if (intFromBuff(bytes, 2, 1) == 150) 15 * 60 else if (intFromBuff(bytes, 2, 1) == 160) 30 * 60 else intFromBuff(bytes, 2, 1) * 60 * 60
        val tempBasalRunningSeconds = intFromBuff(bytes, 3, 3)
        val tempBasalRemainingMin = (tempBasalTotalSec - tempBasalRunningSeconds) / 60
        val tempBasalStart = if (isTempBasalInProgress) getDateFromTempBasalSecAgo(tempBasalRunningSeconds) else 0
        danaRPump.isTempBasalInProgress = isTempBasalInProgress
        danaRPump.tempBasalPercent = tempBasalPercent
        danaRPump.tempBasalRemainingMin = tempBasalRemainingMin
        danaRPump.tempBasalTotalSec = tempBasalTotalSec
        danaRPump.tempBasalStart = tempBasalStart
        aapsLogger.debug(LTag.PUMPCOMM, "Is temp basal running: $isTempBasalInProgress")
        aapsLogger.debug(LTag.PUMPCOMM, "Is APS temp basal running: $isAPSTempBasalInProgress")
        aapsLogger.debug(LTag.PUMPCOMM, "Current temp basal percent: $tempBasalPercent")
        aapsLogger.debug(LTag.PUMPCOMM, "Current temp basal remaining min: $tempBasalRemainingMin")
        aapsLogger.debug(LTag.PUMPCOMM, "Current temp basal total sec: $tempBasalTotalSec")
        aapsLogger.debug(LTag.PUMPCOMM, "Current temp basal start: " + DateUtil.dateAndTimeString(tempBasalStart))
    }

    private fun getDateFromTempBasalSecAgo(tempBasalAgoSecs: Int): Long {
        return (ceil(System.currentTimeMillis() / 1000.0) - tempBasalAgoSecs).toLong() * 1000
    }
}