package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the TemporaryTarget with the specified id
 */
class InvalidateTemporaryTargetTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val tempBasal = database.temporaryTargetDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TemporaryTarget with the specified ID.")
        tempBasal.isValid = false
        database.temporaryTargetDao.updateExistingEntry(tempBasal)
    }
}