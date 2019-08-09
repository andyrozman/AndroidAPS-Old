package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.TotalDailyDose
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.transactions.MealBolusTransaction
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertTotalDailyDoseTransaction(
        val timestamp: Long,
        var basalAmount: Double?,
        var bolusAmount: Double?,
        var totalAmount: Double?,
        val pumpSerial: String,
        pumpId: Int,
        val pumpType: InterfaceIDs.PumpType
) : Transaction<Unit>() {

    val pumpId = pumpId.toLong()

    override fun run() {
        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()

        database.totalDailyDoseDao.insertNewEntry(TotalDailyDose(
                timestamp = timestamp,
                utcOffset = utcOffset,
                basalAmount = basalAmount,
                bolusAmount = bolusAmount,
                totalAmount = totalAmount
        ).apply {
            interfaceIDs.pumpType = pumpType
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = pumpId
        })

    }
}