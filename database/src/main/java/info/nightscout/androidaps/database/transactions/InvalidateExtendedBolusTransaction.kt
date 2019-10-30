package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the ExtendedBolus with the specified id
 */
class InvalidateExtendedBolusTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val extendedBolus = database.extendedBolusDao.findById(id)
                ?: throw IllegalArgumentException("There is no such ExtendedBolus with the specified ID.")
        extendedBolus.isValid = false
        database.extendedBolusDao.updateExistingEntry(extendedBolus)
    }
}