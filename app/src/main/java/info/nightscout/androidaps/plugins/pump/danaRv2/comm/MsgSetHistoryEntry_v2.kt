package info.nightscout.androidaps.plugins.pump.danaRv2.comm

import info.nightscout.androidaps.logging.AAPSLogger
import info.nightscout.androidaps.logging.LTag
import info.nightscout.androidaps.plugins.pump.danaR.comm.MessageBase
import java.util.*

class MsgSetHistoryEntry_v2(
    private val aapsLogger: AAPSLogger,
    type: Int, time: Long, param1: Int, param2: Int
) : MessageBase() {

    init {
        SetCommand(0xE004)
        AddParamByte(type.toByte())
        val gtime = GregorianCalendar()
        gtime.timeInMillis = time
        AddParamDateTime(gtime)
        AddParamInt(param1)
        AddParamInt(param2)
        aapsLogger.debug(LTag.PUMPCOMM, "Set history entry: type: " + type + " date: " + Date(time).toString() + " param1: " + param1 + " param2: " + param2)
    }

    override fun handleMessage(bytes: ByteArray) {
        val result = intFromBuff(bytes, 0, 1)
        if (result != 1) {
            failed = true
            aapsLogger.debug(LTag.PUMPCOMM, "Set history entry result: $result FAILED!!!")
        } else {
            failed = false
            aapsLogger.debug(LTag.PUMPCOMM, "Set history entry result: $result")
        }
    }
}