package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.entities.TherapyEvent
import java.util.*

/**
 * Inserts a TherapyEvent into the database.
 * If type is ACTIVITY or APS_OFFLINE, it will cancel the active at the specified timestamp
 *      if there is one by adjusting the duration property.
 * If type is ACTIVITY or APS_OFFLINE and duration is 0, no new entry will be inserted.
 */
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