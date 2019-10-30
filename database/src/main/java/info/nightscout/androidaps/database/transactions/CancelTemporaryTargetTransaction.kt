package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.exception.NoActiveEntryException

/**
 * Cancels the TemporaryTarget active at the specified timestamp by adjusting the duration property
 * @throws NoActiveEntryException If there is no active entry
 */
class CancelTemporaryTargetTransaction(private val timestamp: Long = System.currentTimeMillis()) : Transaction<Unit>() {

    override fun run() {
        val currentlyActive = database.temporaryTargetDao.getTemporaryTargetActiveAt(timestamp)
                ?: throw NoActiveEntryException("There is no TemporaryTarget active at the specified timestamp.")
        currentlyActive.duration = timestamp - currentlyActive.timestamp
        database.temporaryTargetDao.updateExistingEntry(currentlyActive)
    }
}