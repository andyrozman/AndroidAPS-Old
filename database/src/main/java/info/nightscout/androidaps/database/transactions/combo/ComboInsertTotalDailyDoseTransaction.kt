package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.TotalDailyDose
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class ComboInsertTotalDailyDoseTransaction(
        val tdds: List<TDD>,
        val pumpSerial: String
) : Transaction<Unit>() {

    override fun run() {
        tdds.forEach {
            database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
                    utcOffset = TimeZone.getDefault().getOffset(it.timestamp).toLong(),
                    timestamp = it.timestamp,
                    bolusAmount = null,
                    basalAmount = null,
                    totalAmount = it.totalAmount
            ).apply {
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_COMBO
            })
        }
    }

    data class TDD(val timestamp: Long, val totalAmount: Double)
}