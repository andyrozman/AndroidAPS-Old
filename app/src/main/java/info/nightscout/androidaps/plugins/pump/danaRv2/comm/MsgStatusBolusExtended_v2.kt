package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.danaR.DanaRPump
import info.nightscout.androidaps.plugins.pump.danaR.comm.MessageBase
import kotlin.math.ceil

class MsgStatusBolusExtended_v2(
    private val aapsLogger: AAPSLogger,
    private val danaRPump: DanaRPump
) : MessageBase() {

    init {
        SetCommand(0x0207)
        aapsLogger.debug(LTag.PUMPCOMM, "New message")
    }

    override fun handleMessage(bytes: ByteArray) {
        val isExtendedInProgress = intFromBuff(bytes, 0, 1) == 1
        val extendedBolusHalfHours = intFromBuff(bytes, 1, 1)
        val extendedBolusMinutes = extendedBolusHalfHours * 30
        val extendedBolusAmount = intFromBuff(bytes, 2, 2) / 100.0
        val extendedBolusSoFarInSecs = intFromBuff(bytes, 4, 3)
        // This is available only on korean, but not needed now
//        int extendedBolusDeliveryPulse = intFromBuff(bytes, 7, 2);
//        int isEasyUIUserSleep = intFromBuff(bytes, 9, 1);
        val extendedBolusSoFarInMinutes = extendedBolusSoFarInSecs / 60
        val extendedBolusAbsoluteRate = if (isExtendedInProgress) extendedBolusAmount / extendedBolusMinutes * 60 else 0.0
        val extendedBolusStart = if (isExtendedInProgress) getDateFromSecAgo(extendedBolusSoFarInSecs) else 0
        val extendedBolusRemainingMinutes = extendedBolusMinutes - extendedBolusSoFarInMinutes
        danaRPump.isExtendedInProgress = isExtendedInProgress
        danaRPump.extendedBolusMinutes = extendedBolusMinutes
        danaRPump.extendedBolusAmount = extendedBolusAmount
        danaRPump.extendedBolusSoFarInMinutes = extendedBolusSoFarInMinutes
        danaRPump.extendedBolusAbsoluteRate = extendedBolusAbsoluteRate
        danaRPump.extendedBolusStart = extendedBolusStart
        danaRPump.extendedBolusRemainingMinutes = extendedBolusRemainingMinutes
        aapsLogger.debug(LTag.PUMPCOMM, "Is extended bolus running: $isExtendedInProgress")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus min: $extendedBolusMinutes")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus amount: $extendedBolusAmount")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus so far in minutes: $extendedBolusSoFarInMinutes")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus absolute rate: $extendedBolusAbsoluteRate")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus start: $extendedBolusStart")
        aapsLogger.debug(LTag.PUMPCOMM, "Extended bolus remaining minutes: $extendedBolusRemainingMinutes")
    }

    private fun getDateFromSecAgo(tempBasalAgoSecs: Int): Long {
        return (ceil(System.currentTimeMillis() / 1000.0) - tempBasalAgoSecs).toLong() * 1000
    }
}