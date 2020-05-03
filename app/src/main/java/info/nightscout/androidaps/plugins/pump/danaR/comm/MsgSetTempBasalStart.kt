package info.nightscout.androidaps.plugins.pump.danaR.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag

class MsgSetTempBasalStart(
    private val aapsLogger: AAPSLogger,
    private var percent: Int,
    private var durationInHours: Int
) : MessageBase() {

    init {
        SetCommand(0x0401)

        //HARDCODED LIMITS
        if (percent < 0) percent = 0
        if (percent > 200) percent = 200
        if (durationInHours < 1) durationInHours = 1
        if (durationInHours > 24) durationInHours = 24
        AddParamByte((percent and 255).toByte())
        AddParamByte((durationInHours and 255).toByte())
        aapsLogger.debug(LTag.PUMPCOMM, "Temp basal start percent: $percent duration hours: $durationInHours")
    }

    override fun handleMessage(bytes: ByteArray) {
        val result = intFromBuff(bytes, 0, 1)
        if (result != 1) {
            failed = true
            aapsLogger.debug(LTag.PUMPCOMM, "Set temp basal start result: $result FAILED!!!")
        } else {
            failed = false
            aapsLogger.debug(LTag.PUMPCOMM, "Set temp basal start result: $result")
        }
    }
}