package info.nightscout.androidaps.database.transactions

/**
 * Invalidates the TherapyEvent with the specified id
 */
class InvalidateTherapyEventTransaction(val id: Long) : Transaction<Unit>() {

    override fun run() {
        val therapyEvent = database.therapyEventDao.findById(id)
                ?: throw IllegalArgumentException("There is no such TherapyEvent with the specified ID.")
        therapyEvent.isValid = false
        database.therapyEventDao.updateExistingEntry(therapyEvent)
    }
}