package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.transactions.MealBolusTransaction
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertTotalDailyDoseTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val insulin: Double,
        val carbs: Double,
        val type: Bolus.Type,
        pumpId: Int,
        val pumpType: InterfaceIDs.PumpType,
        val bolusCalculatorResult: MealBolusTransaction.BolusCalculatorResult?
) : Transaction<Unit>() {

    val pumpId = pumpId.toLong()

    override fun run() {
        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
        var entries = 1
        val bolusDBId = database.bolusDao.insertNewEntry(Bolus(
                timestamp = timestamp,
                utcOffset = utcOffset,
                amount = insulin,
                type = type,
                basalInsulin = false
        ).apply {
            interfaceIDs.pumpType = pumpType
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = pumpId
            changes.add(this)
        })
        val bolusCalculatorResultDBId = if (bolusCalculatorResult != null) {
            entries += 1

            database.bolusCalculatorResultDao.insertNewEntry(info.nightscout.androidaps.database.entities.BolusCalculatorResult(
                    timestamp = timestamp,
                    utcOffset = utcOffset,
                    targetBGLow = bolusCalculatorResult.targetBGLow,
                    targetBGHigh = bolusCalculatorResult.targetBGHigh,
                    isf = bolusCalculatorResult.isf,
                    ic = bolusCalculatorResult.ic,
                    bolusIOB = bolusCalculatorResult.bolusIOB,
                    bolusIOBUsed = bolusCalculatorResult.bolusIOBUsed,
                    basalIOB = bolusCalculatorResult.basalIOB,
                    basalIOBUsed = bolusCalculatorResult.basalIOBUsed,
                    glucoseValue = bolusCalculatorResult.glucoseValue,
                    glucoseUsed = bolusCalculatorResult.glucoseUsed,
                    glucoseDifference = bolusCalculatorResult.glucoseDifference,
                    glucoseInsulin = bolusCalculatorResult.glucoseInsulin,
                    glucoseTrend = bolusCalculatorResult.glucoseTrend,
                    trendUsed = bolusCalculatorResult.trendUsed,
                    trendInsulin = bolusCalculatorResult.trendInsulin,
                    carbs = bolusCalculatorResult.carbs,
                    carbsUsed = bolusCalculatorResult.carbsUsed,
                    carbsInsulin = bolusCalculatorResult.carbsInsulin,
                    otherCorrection = bolusCalculatorResult.otherCorrection,
                    superbolusUsed = bolusCalculatorResult.superbolusUsed,
                    superbolusInsulin = bolusCalculatorResult.superbolusInsulin,
                    tempTargetUsed = bolusCalculatorResult.tempTargetUsed,
                    totalInsulin = bolusCalculatorResult.totalInsulin,
                    cob = bolusCalculatorResult.cob,
                    cobUsed = bolusCalculatorResult.cobUsed,
                    cobInsulin = bolusCalculatorResult.cobInsulin
            ).apply {
                changes.add(this)
            })
        } else {
            null
        }
        val carbsDBId = if (carbs > 0) {
            entries += 1
            database.carbsDao.insertNewEntry(Carbs(
                    timestamp = timestamp,
                    utcOffset = utcOffset,
                    amount = carbs,
                    duration = 0
            ).apply {
                changes.add(this)
            })
        } else {
            null
        }
        if (entries > 1) {
            database.mealLinkDao.insertNewEntry(MealLink(
                    bolusId = bolusDBId,
                    carbsId = carbsDBId,
                    bolusCalcResultId = bolusCalculatorResultDBId
            ).apply {
                changes.add(this)
            })
        }
    }
}