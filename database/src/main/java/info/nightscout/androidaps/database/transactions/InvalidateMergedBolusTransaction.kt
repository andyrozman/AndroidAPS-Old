package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the backing entries of a MergedBolus
 */
class InvalidateMergedBolusTransaction(val mergedBolus: MergedBolus) : Transaction<Unit>() {
    override fun run() {
        mergedBolus.bolus?.let {
            val bolus = database.bolusDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such Bolus with the specified ID.")
            bolus.isValid = false
            database.bolusDao.updateExistingEntry(bolus)
        }
        mergedBolus.carbs?.let {
            val carbs = database.carbsDao.findById(it.id)
                    ?: throw IllegalArgumentException("There are no such Carbs with the specified ID.")
            carbs.isValid = false
            database.carbsDao.updateExistingEntry(carbs)
        }
        mergedBolus.mealLink?.let {
            val mealLink = database.mealLinkDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such MealLink with the specified ID.")
            mealLink.isValid = false
            database.mealLinkDao.updateExistingEntry(mealLink)
        }
        mergedBolus.bolusCalculatorResult?.let {
            val bolusCalculatorResult = database.bolusCalculatorResultDao.findById(it.id)
                    ?: throw IllegalArgumentException("There is no such BolusCalculatorResult with the specified ID.")
            bolusCalculatorResult.isValid = false
            database.bolusCalculatorResultDao.updateExistingEntry(bolusCalculatorResult)
        }
    }
}