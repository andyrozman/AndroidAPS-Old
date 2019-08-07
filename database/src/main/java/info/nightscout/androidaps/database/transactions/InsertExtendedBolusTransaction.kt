package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.entities.ExtendedBolus
import java.util.*

class InsertExtendedBolusTransaction(val timestamp: Long, val duration: Long, val amount: Double) : Transaction<Unit>() {
    override fun run() {
        database.extendedBolusDao.insertNewEntry(ExtendedBolus(
                timestamp = timestamp,
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                amount = amount,
                duration = duration,
                emulatingTempBasal = false
        ))
    }
}