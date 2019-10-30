package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the TemporaryBasal with the specified id
 */
class InvalidateTemporaryBasalTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val tempBasal = database.temporaryBasalDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TemporaryBasal with the specified ID.")
        tempBasal.isValid = false
        database.temporaryBasalDao.updateExistingEntry(tempBasal)
    }
}