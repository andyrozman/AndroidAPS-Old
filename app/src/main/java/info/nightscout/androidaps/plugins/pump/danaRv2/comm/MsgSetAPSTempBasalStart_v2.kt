package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.danaR.comm.MessageBase

class MsgSetAPSTempBasalStart_v2(
    private val aapsLogger: AAPSLogger,
    private var percent: Int,
    fifteenMinutes: Boolean,
    thirtyMinutes: Boolean
) : MessageBase() {

    val PARAM30MIN = 160
    val PARAM15MIN = 150

    init {
        SetCommand(0xE002)
        //HARDCODED LIMITS
        if (percent < 0) percent = 0
        if (percent > 500) percent = 500
        AddParamInt(percent)
        if (thirtyMinutes && percent <= 200) { // 30 min is allowed up to 200%
            AddParamByte(PARAM30MIN.toByte())
            aapsLogger.debug(LTag.PUMPCOMM, "APS Temp basal start percent: $percent duration 30 min")
        } else {
            AddParamByte(PARAM15MIN.toByte())
            aapsLogger.debug(LTag.PUMPCOMM, "APS Temp basal start percent: $percent duration 15 min")
        }
    }

    override fun handleMessage(bytes: ByteArray) {
        val result = intFromBuff(bytes, 0, 1)
        if (result != 1) {
            failed = true
            aapsLogger.debug(LTag.PUMPCOMM, "Set APS temp basal start result: $result FAILED!!!")
        } else {
            failed = false
            aapsLogger.debug(LTag.PUMPCOMM, "Set APS temp basal start result: $result")
        }
    }
}