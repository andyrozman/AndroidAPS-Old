package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction

class PumpCancelTemporaryBasalTransaction(
        val timestamp: Long = System.currentTimeMillis(),
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String
): Transaction<Unit>() {

    override fun run() {
        val currentlyActive = database.temporaryBasalDao.getTemporaryBasalActiveAtForPump(timestamp, pumpType, pumpSerial)
                ?: throw IllegalStateException("There is currently no TemporaryBasal active.")
        currentlyActive.duration = timestamp - currentlyActive.timestamp
        database.temporaryBasalDao.updateExistingEntry(currentlyActive)
    }
}