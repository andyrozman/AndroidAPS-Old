package info.nightscout.androidaps.database.transactions.combo

import info.nightscout.androidaps.database.embedments.InterfaceIDs
import info.nightscout.androidaps.database.entities.Bolus
import info.nightscout.androidaps.database.entities.Carbs
import info.nightscout.androidaps.database.entities.links.MealLink
import info.nightscout.androidaps.database.transactions.Transaction
import java.util.*

class ComboMealBolusTransaction(
        val pumpSerial: String,
        val timestamp: Long,
        val insulin: Double,
        val carbs: Double,
        val bolusId: Long,
        val smb: Boolean
) : Transaction<Boolean>() {

    override fun run(): Boolean {
        val bolusExistsInDb = database.bolusDao
                .findByPumpId(InterfaceIDs.PumpType.ACCU_CHEK_COMBO, pumpSerial, bolusId) != null
        if (bolusExistsInDb) return false

        val utcOffset = TimeZone.getDefault().getOffset(timestamp).toLong()
        val bolusDBId = createBolusRecord(utcOffset)
        if (carbs > 0) {
            val carbsDBId = createCarbsRecord(utcOffset)
            linkBolusAndCarbsRecord(bolusDBId, carbsDBId)
        }
        return true
    }

    private fun createBolusRecord(utcOffset: Long): Long {
        return database.bolusDao.insertNewEntry(Bolus(
                timestamp = timestamp,
                utcOffset = utcOffset,
                amount = insulin,
                type = if (smb) Bolus.Type.SMB else Bolus.Type.NORMAL,
                isBasalInsulin = false
        ).apply {
            interfaceIDs.pumpType = InterfaceIDs.PumpType.ACCU_CHEK_COMBO
            interfaceIDs.pumpSerial = pumpSerial
            interfaceIDs.pumpId = bolusId
        })
    }

    private fun createCarbsRecord(utcOffset: Long): Long {
        return database.carbsDao.insertNewEntry(Carbs(
                timestamp = timestamp,
                utcOffset = utcOffset,
                amount = carbs,
                duration = 0
        ))
    }

    private fun linkBolusAndCarbsRecord(bolusDBId: Long, carbsDBId: Long) {
        database.mealLinkDao.insertNewEntry(MealLink(
                bolusId = bolusDBId,
                carbsId = carbsDBId
        ))
    }
}