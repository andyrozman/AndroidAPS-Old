package info.nightscout.androidaps.database.transactions.insight

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.transactions.MealBolusTransaction
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class InsightMealBolusTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val insulin: Double,
        val carbs: Double,
        bolusId: Int,
        val type: Bolus.Type,
        val bolusCalculatorResult: MealBolusTransaction.BolusCalculatorResult?
) : Transaction<Unit>() {

    val bolusId = bolusId.toLong()

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
            interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_INSIGHT
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = bolusId
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
            ))
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
            ))
        } else {
            null
        }
        if (entries > 1) {
            database.mealLinkDao.insertNewEntry(MealLink(
                    bolusId = bolusDBId,
                    carbsId = carbsDBId,
                    bolusCalcResultId = bolusCalculatorResultDBId
            ))
        }
    }
}