package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.TherapyEvent
import java.util.*

class TherapyEventTransaction(
        val timestamp: Long,
        val type: TherapyEvent.Type,
        val amount: Double?,
        val note: String?,
        val duration: Long
) : Transaction<Unit>() {
    override fun run() {
        if (type == TherapyEvent.Type.ACTIVITY || type == TherapyEvent.Type.APS_OFFLINE) {
            val currentlyActive = database.therapyEventDao.getTherapyEventActiveAt(type, timestamp)
            if (currentlyActive != null) {
                currentlyActive.duration = timestamp - currentlyActive.timestamp
                database.therapyEventDao.updateExistingEntry(currentlyActive)
            }
            if (duration != 0L) {
                database.therapyEventDao.insertNewEntry(TherapyEvent(
                        timestamp = timestamp,
                        utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                        duration = duration,
                        type = type
                ))
            }
        } else {
            database.therapyEventDao.insertNewEntry(TherapyEvent(
                    timestamp = timestamp,
                    utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                    duration = 0,
                    note = note,
                    amount = amount,
                    type = type
            ))
        }
    }
}