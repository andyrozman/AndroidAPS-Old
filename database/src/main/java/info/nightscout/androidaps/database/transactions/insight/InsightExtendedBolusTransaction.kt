package info.nightscout.androidaps.database.transactions.insight

import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.ExtendedBolus
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class InsightExtendedBolusTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val amount: Double,
        val duration: Long,
        bolusId: Int,
        val emulatingTempBasal: Boolean
) : Transaction<Unit>() {

    val bolusId = bolusId.toLong()

    override fun run() {
        database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                amount = amount,
                duration = duration,
                isEmulatingTempBasal = emulatingTempBasal
        ).apply {
            interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = bolusId
        })
    }
}