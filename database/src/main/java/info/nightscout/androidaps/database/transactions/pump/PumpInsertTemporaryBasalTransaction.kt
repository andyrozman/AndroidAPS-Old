package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.AppRepository.database
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertTemporaryBasalTransaction(
        val timestamp: Long,
        val durationMs: Long = 0,
        val durationMin: Long = 0,
        val absolute: Boolean,
        val rate: Double,
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String,
        val pumpId: Long = 0
) : Transaction<Unit>() {

    override fun run() {

        var duration = 0L

        if (durationMs>0)
            duration = durationMs
        else
            duration = durationMin * 60000L

        database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                type = TemporaryBasal.Type.NORMAL,
                absolute = absolute,
                rate = rate,
                duration = duration
        ).apply {
            interfaceIDs.pumpType = pumpType
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = pumpId
        })
    }
}