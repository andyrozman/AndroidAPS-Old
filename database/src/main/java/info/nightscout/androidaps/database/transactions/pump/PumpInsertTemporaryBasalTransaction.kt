package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.AppRepository.database
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertTemporaryBasalTransaction(
        val timestamp: Long,
        val duration: Long,
        val absolute: Boolean,
        val rate: Double,
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String,
        pumpId: Int = 0
) : Transaction<Unit>() {

    val pumpId = pumpId.toLong()

    override fun run() {

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
            changes.add(this)
        })
    }
}