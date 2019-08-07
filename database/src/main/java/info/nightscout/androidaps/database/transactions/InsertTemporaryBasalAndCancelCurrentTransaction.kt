package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.TemporaryBasal
import java.util.*

class InsertTemporaryBasalAndCancelCurrentTransaction(val timestamp: Long, val duration: Long, val absolute: Boolean, val rate: Double) : Transaction<Unit>() {
    override fun run() {
        val currentlyActive = database.temporaryBasalDao.getTemporaryBasalActiveAt(timestamp)
        if (currentlyActive != null) {
            currentlyActive.duration = timestamp - currentlyActive.timestamp
            database.temporaryBasalDao.updateExistingEntry(currentlyActive)
        }
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