package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.ExtendedBolus
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpExtendedBolusTransaction(
        val timestamp: Long,
        val amount: Double,
        val durationMs: Long = 0,
        val durationMin: Int = 0,
        val emulatingTempBasal: Boolean,
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String,
        val pumpId: Long = 0
) : Transaction<Unit>() {



    override fun run() {
        var duration: Long

        if (durationMs>0)
            duration = durationMs
        else
            duration = durationMin * 60000L

        database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                amount = amount,
                duration = duration,
                emulatingTempBasal = emulatingTempBasal
        ).apply {
            interfaceIDs.pumpType = pumpType
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = pumpId
        })
    }
}