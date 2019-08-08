package info.nightscout.androidaps.database.transactions.pump

import info.nightscout.androidaps.database.AppRepository
import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.TemporaryBasal
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.transactions.MealBolusTransaction
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class PumpInsertUpdateBolusTransaction(
        val timestamp: Long,
        val insulin: Double,
        val carbs: Double,
        val type: Bolus.Type,
        val pumpType: InterfaceIDs.PumpType,
        val pumpSerial: String,
        var pumpId: Long = 0,
        val bolusCalculatorResult: MealBolusTransaction.BolusCalculatorResult?
) : Transaction<Unit>() {

    override fun run() {

        var currentBolus : Bolus? = null

        if (pumpId > 0) {
            currentBolus = database.bolusDao.getBolusByPumpId(pumpType, pumpSerial, pumpId)
        } else {
            currentBolus = database.bolusDao.getBolusByTimeAndPump(timestamp, pumpType, pumpSerial)
        }

        if (currentBolus==null) {

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
                })
            }
        } else {

            val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
            var entries = 1
            val bolusDBId = database.bolusDao.updateExistingEntry(Bolus(
                    timestamp = currentBolus.timestamp,
                    utcOffset = currentBolus.utcOffset,
                    amount = insulin,
                    type = currentBolus.type,
                    basalInsulin = currentBolus.basalInsulin
            ).apply {
                interfaceIDs.pumpType = pumpType
                interfaceIDs.pumpSerial = pumpSerial
                interfaceIDs.pumpId = pumpId
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
                })
            }

        }
    }
}