package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.transactions.Transaction

class ComboCancelTempBasalTransaction: Transaction<Unit>() {

    override fun run() {
        val now = System.currentTimeMillis()
        val currentlyActive = database.temporaryBasalDao
                .getTemporaryBasalActiveAtIncludingInvalid(now, InterfaceIDs.PumpType.ACCU_CHEK_COMBO)
                ?: throw IllegalStateException("There is currently no TemporaryBasal active.")
        currentlyActive.duration = now - currentlyActive.timestamp
        database.temporaryBasalDao.updateExistingEntry(currentlyActive)
    }
}
