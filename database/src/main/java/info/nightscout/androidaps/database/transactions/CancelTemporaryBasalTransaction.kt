package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.exception.NoActiveEntryException

/**
 * Cancels the TemporaryBasal active at the specified timestamp by adjusting the duration property
 * @throws NoActiveEntryException If there is no active entry
 */
class CancelTemporaryBasalTransaction(private val timestamp: Long = System.currentTimeMillis()) : Transaction<Unit>() {

    override fun run() {
        val currentlyActive = database.temporaryBasalDao.getTemporaryBasalActiveAt(timestamp)
                ?: throw NoActiveEntryException("There is no TemporaryBasal active at the specified timestamp.")
        currentlyActive.duration = timestamp - currentlyActive.timestamp
        database.temporaryBasalDao.updateExistingEntry(currentlyActive)
    }
}