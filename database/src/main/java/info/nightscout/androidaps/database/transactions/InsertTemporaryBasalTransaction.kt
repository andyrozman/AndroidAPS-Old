package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.TemporaryBasal
import java.util.*

class InsertTemporaryBasalTransaction(val timestamp: Long, val duration: Long, val absolute: Boolean, val rate: Double) : Transaction<Unit>() {
    override fun run() {
        database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                type = TemporaryBasal.Type.NORMAL,
                absolute = absolute,
                rate = rate,
                duration = duration
        ))
    }
}