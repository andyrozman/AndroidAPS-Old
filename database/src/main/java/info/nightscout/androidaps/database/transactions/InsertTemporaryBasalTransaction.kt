package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.entities.TemporaryBasal
import java.util.*

/**
 * Inserts a TemporaryBasal into the database
 */
class InsertTemporaryBasalTransaction(val timestamp: Long, val duration: Long, val absolute: Boolean, val rate: Double) : Transaction<Unit>() {
    override fun run() {
        database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                type = TemporaryBasal.Type.NORMAL,
                isAbsolute = absolute,
                rate = rate,
                duration = duration
        ))
    }
}