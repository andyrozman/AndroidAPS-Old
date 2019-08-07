package info.nightscout.androidaps.database.transactions

import info.nightscout.androidaps.database.AppRepository

class InvalidateMergedBolusTransaction(val mergedBolus: MergedBolus) : Transaction<Unit>() {
    override fun run() {
        mergedBolus.bolus?.let {
            val bolus = database.bolusDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such Bolus with the specified ID.")
            bolus.valid = false
            database.bolusDao.updateExistingEntry(bolus)
        }
        mergedBolus.carbs?.let {
            val carbs = database.carbsDao.findById(it.id)
                    ?: throw IllegalArgumentException("There are no such Carbs with the specified ID.")
            carbs.valid = false
            database.carbsDao.updateExistingEntry(carbs)
        }
        mergedBolus.mealLink?.let {
            val mealLink = database.mealLinkDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such MealLink with the specified ID.")
            mealLink.valid = false
            database.mealLinkDao.updateExistingEntry(mealLink)
        }
        mergedBolus.bolusCalculatorResult?.let {
            val bolusCalculatorResult = database.bolusCalculatorResultDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such BolusCalculatorResult with the specified ID.")
            bolusCalculatorResult.valid = false
            database.bolusCalculatorResultDao.updateExistingEntry(bolusCalculatorResult)
        }
    }
}