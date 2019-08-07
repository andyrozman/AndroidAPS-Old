package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class CancelTemporaryBasalTransaction(val timestamp: Long = System.currentTimeMillis()): Transaction<Unit>() {

    override fun run() {
        val currentlyActive = database.temporaryBasalDao.getTemporaryBasalActiveAt(timestamp)
                ?: throw IllegalStateException("There is currently no TemporaryBasal active.")
        currentlyActive.duration = timestamp - currentlyActive.timestamp
        database.temporaryBasalDao.updateExistingEntry(currentlyActive)
    }
}