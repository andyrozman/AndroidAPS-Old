package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class ComboNewTempBasalTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val percentage: Int,
        val duration: Int
) : Transaction<Unit>() {

    override fun run() {
        database.temporaryBasalDao.insertNewEntry(TemporaryBasal(
                utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong(),
                timestamp = timestamp,
                isAbsolute = false,
                rate = percentage.toDouble(),
                duration = duration.toLong(),
                type = TemporaryBasal.Type.NORMAL
        ))
    }
}

