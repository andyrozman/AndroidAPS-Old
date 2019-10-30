package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.exception.NoActiveEntryException

/**
 * Cancels the ExtendedBolus active at the specified timestamp by adjusting the duration property
 * @throws NoActiveEntryException If there is no active ExtendedBolus
 */
class CancelExtendedBolusTransaction(private val timestamp: Long = System.currentTimeMillis()) : Transaction<Unit>() {

    override fun run() {
        val currentlyActive = database.extendedBolusDao.getExtendedBolusActiveAt(timestamp)
                ?: throw NoActiveEntryException("There is no ExtendedBolus active at the specified timestamp.")
        currentlyActive.duration = timestamp - currentlyActive.timestamp
        database.extendedBolusDao.updateExistingEntry(currentlyActive)
    }
}